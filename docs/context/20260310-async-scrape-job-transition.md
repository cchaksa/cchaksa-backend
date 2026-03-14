# Context: 스크래핑 연동의 비동기 Job 기반 전환

## 1. Feature Overview (Required)
- Purpose: 기존 동기식 스크래핑 연동을 비동기 job 기반 구조로 전환해, 백엔드가 job 상태와 최종 저장의 source of truth가 되도록 만든다.
- Scope:
  - In:
    - 스크래핑 요청 API를 `accepted + job_id` 응답 구조로 전환
    - job 상태 조회 API 추가
    - 내부 callback API 추가 및 HMAC 검증 구현
    - 스크래핑 job 상태 모델/엔티티/저장소 추가
    - 기존 동기 HTTP 호출 로직을 포트/어댑터 구조로 분리
    - SQS enqueue 기반 비동기 경로 추가
    - `develop-shadow` 또는 설정값 기반 병행 운영 구조 추가
    - 상태 전이, idempotency, 권한 체크, 테스트 추가
  - Out:
    - Terraform 및 AWS 인프라 리소스 생성/수정
    - 스크래핑 worker 구현 자체
    - 프론트엔드 polling UI 구현
    - 운영 환경 즉시 cutover
- Expected Impact: 백엔드는 요청 즉시 수락하고 job 상태를 관리하며, 결과 반영은 callback을 통해 비동기로 수행한다. 기존 동기 HTTP 진입 엔드포인트는 비활성화되고, 스크래핑 워커와의 연동은 SQS + callback 계약으로 고정된다.
- Stakeholder Confirmation: Requirement provided by requester on 2026-03-10 for "스크래핑 연동을 기존 동기 HTTP 호출 방식에서 완전 비동기 job 기반 방식으로 전환" with parallel operation safety.

## 2. Domain Rules (Highest Priority, Required)
- Rule 1: 스크래핑 job의 단일 source of truth는 백엔드 DB이며, worker는 DB에 직접 접근하지 않는다.
- Rule 2: `POST /portal/link`의 성공 응답은 즉시 완료 결과가 아니라 `accepted + job_id + polling_endpoint`여야 한다.
- Rule 3: job 상태는 `queued -> running -> succeeded|failed` 흐름을 따르며, 완료 상태(`succeeded`, `failed`)로 전이된 job은 다시 미완료 상태로 되돌아가면 안 된다.
- Rule 4: 동일 `job_id` callback이 중복 도착해도 최종 반영은 한 번만 이뤄져야 하며, 중복 callback은 안전하게 무시되거나 `409`로 처리되어야 한다.
- Rule 5: callback 검증은 raw body 기준 `HMAC-SHA256`으로 수행하며, canonical string은 `${timestamp}.${rawBody}`를 사용한다.
- Rule 6: job 조회 API는 사용자 본인 job만 조회 가능해야 하며 내부 에러 상세나 민감한 payload를 외부에 그대로 노출하면 안 된다.
- Rule 7: 비동기 경로는 `develop-shadow` 또는 명시적 설정(`scraping.mode=async`)으로 제어하며, 외부 진입 엔드포인트는 `/portal/link` 계열만 사용한다. 기존 `/api/suwon-scrape/*` 엔드포인트는 비활성화한다.
- Rule 8: 백엔드는 worker와 합의된 표준 에러 코드를 저장할 수 있어야 하며, 프론트에는 필요한 범위만 노출한다.

- Mutable Rules:
  - job payload 저장 포맷(JSON string, JSONB, DTO 직렬화)은 구현 중 조정 가능하다.
  - 비동기/동기 분기 방식은 profile 기반 또는 feature flag 기반으로 조정 가능하다.
  - `running` 상태 갱신 방식은 enqueue 시점, 별도 내부 신호, callback 확장 등으로 조정 가능하다.
- Immutable Rules:
  - 백엔드가 job 상태와 최종 저장의 source of truth여야 한다.
  - worker는 DB에 직접 접근하지 않는다.
  - 완료 callback의 idempotency는 백엔드가 책임진다.
  - 운영 안정성을 위해 동기 경로는 병행 유지 가능해야 한다.

## 3. Use-case Scenarios (Required)

### Normal Flow
- Scenario Name: 비동기 스크래핑 요청 수락
  - Trigger: 인증된 사용자가 `POST /portal/link`를 호출한다.
  - Actor: User / PortalLinkController / PortalLinkJobService / ScrapeJobEnqueueService
  - Steps:
    1. 백엔드가 요청 payload를 검증한다.
    2. job row를 `queued` 상태로 생성한다.
    3. SQS에 `job_id`, `user_id`, `portal_type`, 요청 식별 payload, `requested_at`을 발행한다.
    4. API는 즉시 `accepted` 응답과 polling endpoint를 반환한다.
  - Expected Result: 사용자는 즉시 `job_id`를 받고, 이후 상태 조회 API로 진행 상황을 확인할 수 있다.

- Scenario Name: 성공 callback 수신 후 최종 반영
  - Trigger: worker가 성공 결과와 함께 `POST /internal/scrape-results`를 호출한다.
  - Actor: InternalScrapeResultController / ScrapeResultCallbackService
  - Steps:
    1. 백엔드가 HMAC와 timestamp를 검증한다.
    2. job_id와 상태를 검증한다.
    3. job이 미완료 상태면 결과 payload를 도메인 엔티티로 매핑하고 DB에 저장한다.
    4. job 상태를 `succeeded`와 `finished_at`으로 갱신한다.
  - Expected Result: 도메인 데이터와 job 상태가 함께 최종 반영된다.

### Exception / Boundary Flow
- Scenario Name: 실패 callback 수신
  - Condition: worker가 실패 결과(`error_code`, `error_message`, `retryable`)를 전달한다.
  - Expected Behavior: job 상태를 `failed`로 전이하고 오류 정보를 저장하며, 최종 엔티티 저장은 수행하지 않는다.

- Scenario Name: 중복 callback 수신
  - Condition: 이미 `succeeded` 또는 `failed`로 완료된 동일 `job_id`에 대해 callback이 다시 도착한다.
  - Expected Behavior: 추가 반영 없이 안전하게 무시하거나 `409`를 반환하며, worker는 이를 성공 의미로 취급할 수 있어야 한다.

- Scenario Name: 타 사용자 job 조회
  - Condition: 사용자가 자신의 것이 아닌 `job_id`를 조회한다.
  - Expected Behavior: 권한 오류 또는 not found 정책 중 하나로 차단되며, 타 사용자 job 존재 여부를 유추할 수 있는 정보는 노출하지 않는다.

- Scenario Name: 잘못된 HMAC callback
  - Condition: `X-Timestamp`, `X-Signature`, raw body 검증이 실패하거나 허용 오차를 초과한다.
  - Expected Behavior: callback을 거부하고 job 상태를 변경하지 않는다.

## 4. Transaction and Consistency Policy (Required)
- Transaction Start Point: 스크래핑 요청은 API 수신 후 job 생성 시점, callback 처리는 callback 수신 후 job 조회 및 상태 전이 시점에 시작한다.
- Transaction End Point: 요청 수락은 job 생성과 enqueue 성공이 논리적으로 확정된 시점, callback은 상태 전이와 엔티티 저장이 함께 완료된 시점에 종료한다.
- Atomicity Scope:
  - 요청 API는 "accepted 응답을 내보낼 수 있는가"를 하나의 논리 단위로 본다.
  - callback 성공 처리에서는 상태 전이와 최종 엔티티 반영이 원자적으로 보장되어야 한다.
  - 완료된 job의 중복 callback은 상태와 최종 저장을 다시 변경하지 않아야 한다.
- Eventual Consistency Allowed:
  - 프론트의 상태 조회는 polling 기반이므로 `queued/running`에서 최종 완료까지의 비동기 지연을 허용한다.
  - 단, 완료 callback이 반영된 후에는 job 상태와 저장 결과가 서로 불일치하면 안 된다.

## 5. API List (Optional / Required When Present)
- Endpoint: `/portal/link`
  - Method: `POST`
  - Request DTO: `portal_type`, `username`, `password`
  - Response DTO: `job_id`, `status`, `polling_endpoint`, 선택적 안내 메시지
  - Authorization: 사용자 인증 필요
  - Idempotency: `Idempotency-Key` 헤더 기준으로 동일 사용자 + 동일 key + 동일 payload면 같은 `job_id`를 재사용한다.

- Endpoint: `/portal/link/jobs/{job_id}`
  - Method: `GET`
  - Request DTO: path variable `job_id`
  - Response DTO: `job_id`, `status`, `error_code`, `error_message`, `updated_at`
  - Authorization: 사용자 인증 필요, 본인 job만 조회 가능
  - Idempotency: Yes

- Endpoint: `/internal/scrape-results`
  - Method: `POST`
  - Request DTO: `job_id`, `status`, 결과 payload 또는 오류 payload, `finished_at`
  - Response DTO: 내부 계약용 단순 성공/중복/검증 실패 응답
  - Authorization: 외부 공개 인증이 아니라 HMAC 기반 내부 계약 검증
  - Idempotency: 동일 `job_id` callback 중복 허용, 최종 반영은 1회

## 6. Exception Policy (Required)
- Error Code: `INVALID_PAYLOAD`
  - Condition: 요청 또는 callback payload 구조가 계약에 맞지 않음
  - Message Convention: worker/백엔드 공통 이해가 가능한 표준 메시지
  - Handling Layer: Controller validation 또는 callback application service
  - User Exposure: 외부 API에는 필요한 범위만 노출

- Error Code: `PORTAL_TIMEOUT`
  - Condition: worker가 포털 연동 시간 초과로 실패 결과를 전달
  - Message Convention: 표준 코드 저장, 상세 원문은 내부 저장 가능
  - Handling Layer: callback 처리 시 job 실패 저장
  - User Exposure: 상태 조회 응답에는 축약된 오류 정보만 노출 가능

- Error Code: `PORTAL_TEMPORARY_UNAVAILABLE`
  - Condition: 일시적 외부 장애
  - Message Convention: retry 가능 여부와 함께 저장
  - Handling Layer: callback 처리
  - User Exposure: 상태 조회에 제한적으로 노출

- Error Code: `PORTAL_AUTH_FAILED`
  - Condition: 잘못된 포털 자격 증명
  - Message Convention: 사용자 액션이 필요한 실패로 구분 가능해야 함
  - Handling Layer: callback 처리
  - User Exposure: 상태 조회에서 사용자 친화적으로 매핑 가능

- Error Code: `PORTAL_ACCOUNT_LOCKED`
  - Condition: 포털 계정 잠금
  - Message Convention: retryable=false와 함께 저장
  - Handling Layer: callback 처리
  - User Exposure: 상태 조회에서 필요한 범위만 노출

- Error Code: `BUSINESS_RULE_VIOLATION`
  - Condition: 결과 반영 시 도메인 규칙 위반
  - Message Convention: 내부 도메인 실패와 외부 장애를 구분 가능해야 함
  - Handling Layer: callback 처리 중 도메인/application 계층
  - User Exposure: 내부 상세는 숨기고 실패 상태만 노출 가능

- Error Code: `CALLBACK_TIMEOUT`
  - Condition: worker 관점에서 callback 호출 시간 초과
  - Message Convention: worker/백엔드 공통 코드 체계 유지
  - Handling Layer: worker 측 재시도 및 백엔드 재수신 계약
  - User Exposure: 직접 노출 대상 아님

- Error Code: `CALLBACK_5XX`
  - Condition: worker가 백엔드 callback API에서 5xx를 받음
  - Message Convention: worker 재시도 판단용 코드
  - Handling Layer: worker/운영 관측
  - User Exposure: 직접 노출 대상 아님

- Error Code: `UNKNOWN_NON_RETRYABLE`
  - Condition: 분류되지 않는 비재시도 실패
  - Message Convention: 기본 fallback 코드
  - Handling Layer: callback 처리
  - User Exposure: 축약된 형태만 노출

- Error Code: `INVALID_CALLBACK_SIGNATURE`
  - Condition: HMAC 서명 또는 timestamp 검증 실패
  - Message Convention: 내부 계약 위반
  - Handling Layer: internal callback controller
  - User Exposure: 외부 미노출

## 7. Phase Checklist
- [x] Phase 1 Context: requirements, domain rules, exception policy fixed
- [ ] Phase 2 Domain: models, services, exceptions, pure tests written
- [ ] Phase 3 Application: orchestration, transactions, repository interface validation
- [ ] Phase 4 Infrastructure: persistence, external integration, technical implementation validated
- [ ] Phase 5 Global/Config: configuration, security, logging impact reviewed
- [ ] Phase 6 API/Controller: endpoints, docs, validation flows confirmed

## 8. Generated File List (Required)
- Path: docs/context/20260310-async-scrape-job-transition.md
  - Description: 스크래핑 비동기 job 전환 작업을 위한 Context 문서
  - Layer: Context documentation
- Path: build.gradle
  - Description: SQS SDK 의존성 추가
  - Layer: Build config
- Path: src/main/java/com/chukchuk/haksa/domain/scrapejob/model/ScrapeJob.java
  - Description: 스크래핑 job 상태를 저장하는 도메인 모델/엔티티
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/domain/scrapejob/model/ScrapeJobStatus.java
  - Description: `queued`, `running`, `succeeded`, `failed` 상태 enum
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/domain/scrapejob/model/ScrapeJobOperationType.java
  - Description: 초기 연동과 재연동 분기용 operation type enum
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/domain/scrapejob/repository/ScrapeJobRepository.java
  - Description: job 조회/저장 인터페이스
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/domain/portal/dto/PortalLinkDto.java
  - Description: 비동기 스크래핑 요청/조회/callback DTO
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/application/portal/PortalLinkJobService.java
  - Description: 요청 수락, idempotency, job 생성과 enqueue를 담당하는 application service
  - Layer: Application
- Path: src/main/java/com/chukchuk/haksa/application/portal/PortalLinkJobQueryService.java
  - Description: 사용자 job 상태 조회 service
  - Layer: Application
- Path: src/main/java/com/chukchuk/haksa/application/portal/ScrapeResultCallbackService.java
  - Description: callback 검증 이후 상태 전이와 최종 저장을 담당하는 service
  - Layer: Application
- Path: src/main/java/com/chukchuk/haksa/application/portal/ScrapeJobMessage.java
  - Description: SQS message body 계약 DTO
  - Layer: Application
- Path: src/main/java/com/chukchuk/haksa/application/portal/ScrapeJobPublisher.java
  - Description: job 발행 포트
  - Layer: Application
- Path: src/main/java/com/chukchuk/haksa/infrastructure/scrapejob/SqsScrapeJobPublisher.java
  - Description: 비동기 경로에서 SQS 메시지를 발행하는 구현체
  - Layer: Infrastructure
- Path: src/main/java/com/chukchuk/haksa/infrastructure/security/HmacSignatureVerifier.java
  - Description: raw body 기반 HMAC-SHA256 검증 구현
  - Layer: Infrastructure
- Path: src/main/java/com/chukchuk/haksa/global/config/ScrapingProperties.java
  - Description: scraping 관련 설정 프로퍼티 바인딩
  - Layer: Global config
- Path: src/main/java/com/chukchuk/haksa/global/exception/code/ErrorCode.java
  - Description: scrape job / callback 관련 에러 코드 추가
  - Layer: Global config
- Path: src/main/java/com/chukchuk/haksa/global/security/SecurityConfig.java
  - Description: 내부 callback endpoint 공개 경로 허용
  - Layer: Global config
- Path: src/main/java/com/chukchuk/haksa/domain/portal/controller/PortalLinkController.java
  - Description: 비동기 수락 응답을 반환하는 스크래핑 요청 controller
  - Layer: API/Controller
- Path: src/main/java/com/chukchuk/haksa/domain/portal/controller/PortalJobQueryController.java
  - Description: polling용 job 상태 조회 controller
  - Layer: API/Controller
- Path: src/main/java/com/chukchuk/haksa/domain/portal/controller/InternalScrapeResultController.java
  - Description: 내부 callback endpoint controller
  - Layer: API/Controller
- Path: src/main/resources/application.yml
  - Description: `scraping.mode`, queue url, callback secret 등 설정 키 추가
  - Layer: Global config
- Path: src/main/resources/application-develop-shadow.yml
  - Description: shadow 환경에서 async 경로를 활성화하는 설정
  - Layer: Global config
- Path: src/test/resources/application-test.yml
  - Description: scrape callback/queue 테스트 설정 추가
  - Layer: Test config
- Path: src/test/java/com/chukchuk/haksa/application/portal/PortalLinkJobServiceUnitTests.java
  - Description: idempotency와 enqueue 실패 롤백 보장 검증
  - Layer: Test
- Path: src/test/java/com/chukchuk/haksa/application/portal/ScrapeResultCallbackServiceUnitTests.java
  - Description: callback HMAC, 성공/실패/중복 처리 검증
  - Layer: Test
- Path: src/test/java/com/chukchuk/haksa/domain/portal/controller/PortalLinkControllerApiIntegrationTest.java
  - Description: 요청 수락 응답 검증
  - Layer: Test
- Path: src/test/java/com/chukchuk/haksa/domain/portal/controller/PortalJobQueryControllerApiIntegrationTest.java
  - Description: 상태 조회와 타 사용자 접근 차단 검증
  - Layer: Test
- Path: src/test/java/com/chukchuk/haksa/domain/portal/controller/InternalScrapeResultControllerApiIntegrationTest.java
  - Description: callback controller의 raw body 전달 검증
  - Layer: Test
- Path: src/main/java/com/chukchuk/haksa/application/api/SuwonScrapeController.java
  - Description: 기존 동기 HTTP 스크래핑 엔드포인트 제거
  - Layer: API/Controller
- Path: src/main/java/com/chukchuk/haksa/application/api/docs/SuwonScrapeControllerDocs.java
  - Description: 기존 동기 스크래핑 API 문서 제거
  - Layer: API docs
- Path: src/test/java/com/chukchuk/haksa/application/api/SuwonScrapeControllerApiIntegrationTest.java
  - Description: 기존 동기 스크래핑 API 테스트 제거
  - Layer: Test
