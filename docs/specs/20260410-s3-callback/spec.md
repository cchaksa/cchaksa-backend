# Spec Template

## 1. Feature Overview
- Purpose:
  - 스크래핑 콜백 엔드포인트가 스크래핑 워커가 업로드한 S3 payload를 동기적으로 읽고 검증한 뒤 DB에 반영하도록 한다.
- Scope
  - In:
    - `/internal/scrape-results` 요청 DTO를 `job_id`, `status`, `result_s3_key`, `attempt` 중심으로 재정의
    - 콜백 수신 시 기본 검증 및 상태 저장 로직을 `job_id + attempt` 기반 멱등 처리로 강화
    - S3 client 구성 및 HEAD/GET, JSON/schema 검증, 도메인 검증, DB 반영을 **한 요청 안에서 동기 처리**
    - 예외/로그 정책 정리 및 오류 코드 추가
  - Out:
    - 스크래핑 서버 측 변경(별도 시스템)
    - Frontend UI 변경
    - 장기 보관/아카이브 파이프라인 설계
- Expected Impact:
  - callback 응답을 경량화하여 timeout/중복 콜백 위험 감소
  - 원본 결과 JSON을 S3에서 재조회 가능해 장애 분석 용이
  - 멱등성 보장으로 상태 전이 안정성 향상
- Stakeholder Confirmation:
  - 요청자(백엔드 팀) 구두 승인, 2026-04-10. Phase 2 진행 전 "OK to implement" 추가 확인 예정.

## 2. Domain Rules
- Rule 1: `job_id`는 시스템 내에서 유일하며, 상태 전이는 정의된 life-cycle(`PENDING`→`IN_PROGRESS`→`SUCCEEDED|FAILED`)만 허용한다.
- Rule 2: `result_s3_key`가 없으면 후처리를 진행하지 않으며 요청은 실패로 간주하고 재시도 대상이 된다.
- Rule 3: terminal 상태(`SUCCEEDED`, `FAILED`, `CANCELLED`) 이후 동일 job에 대한 콜백은 멱등 처리한다.
- Mutable Rules:
  - S3 read 실패 시 상태를 `FAILED`로 둘지 재시도 큐로 넘길지 정책 확정 필요.
  - 결과 JSON schema 버전 관리 전략.
- Immutable Rules:
  - callback 엔드포인트는 인증/권한 검증을 유지해야 한다.
  - 직접 payload 저장 로직은 제거된다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 성공 콜백 처리
  - Trigger: 스크래핑 서버가 `job_id`와 `result_s3_key`를 담아 POST 요청
  - Actor: 스크래핑 서버
  - Steps:
    1. 콜백 요청 도착 → DTO 검증 (`job_id`, `status`, `result_s3_key`, `attempt`)
    2. 상태 저장/전이 로그 남기기
    3. S3 HEAD/GET → JSON/schema → 도메인 검증 → DB 반영 → 상태 업데이트
    4. 성공 응답 200 반환
  - Expected Result: 콜백 요청 단일 트랜잭션 내에서 모든 후처리가 완료된다.

### Exception / Boundary Flow
- Scenario Name: duplicate callback
  - Condition: 동일 `job_id`가 이미 terminal 상태
  - Expected Behavior: 멱등 로직이 이전 상태를 유지하고 로그에 Duplicate 기록 후 200 응답

- Scenario Name: S3 read 실패
  - Condition: S3 client 예외, key 존재하지 않음
  - Expected Behavior: 콜백 응답은 성공(기본 저장 완료)이나 내부 후처리 재시도/실패 상태 기록 및 알람

- Scenario Name: DTO 파싱 실패
  - Condition: 필수 필드 누락/형식 오류
  - Expected Behavior: 400 응답, 입력 로그 저장, job 상태 변화 없음

## 4. Transaction / Consistency
- Transaction Start Point: 콜백 요청에서 시그니처 검증/멱등 체크 후 상태 전이를 시작할 때
- Transaction End Point: S3 read, JSON/schema 검증, 도메인 검증, DB 반영까지 완료될 때
- Atomicity Scope: 콜백 요청 전체 (상태 전이 + 결과 반영 + 로그)
- Eventual Consistency Allowed: 없음. 콜백 응답 전에 모든 후처리를 완료한다.

## 5. API List (필요 시)
- Endpoint: `/internal/scrape-results`
  - Method: POST
  - Request DTO:
    ```json
    {
      "job_id": "string",
      "status": "SUCCESS|FAIL|CANCELLED",
      "attempt": 1,
      "result_s3_key": "s3://bucket/key.json" 또는 key 문자열,
      "attempt": 1,
      "metadata": {
        "duration_ms": 1234,
        "scraper_version": "v1"
      }
    }
    ```
  - Response DTO: `{ "accepted": true }`
  - Authorization: 내부 인증(기존 토큰) 유지
  - Idempotency: `job_id` + `status` 조합 기준 멱등

## 6. Exception Policy
- Error Code: `SCRAPE_INVALID_REQUEST`
  - Condition: DTO 필수값 누락 또는 형식 오류
  - Message Convention: "Invalid scrape callback payload"
  - Handling Layer: Controller validation → Global exception handler
  - User Exposure: 내부 시스템만 사용, 로그에서 추적
- Error Code: `SCRAPE_INVALID_S3_KEY`
  - Condition: 허용되지 않은 `result_s3_key` prefix, 형식 위반
  - Handling Layer: Callback 서비스
  - User Exposure: 400 응답, 원인 로그
- Error Code: `SCRAPE_RESULT_S3_FAILED`
  - Condition: S3 HEAD/GET 실패, 제한 횟수 재시도 후 실패
  - Handling Layer: Callback 서비스
  - User Exposure: 5xx 응답
- Error Code: `SCRAPE_S3_FAILURE`
  - Condition: S3에서 payload 조회 실패
  - Message Convention: "Failed to fetch scrape result from S3"
  - Handling Layer: 후처리 서비스
  - User Exposure: 내부 로그 + 알람
- Error Code: `SCRAPE_RESULT_SCHEMA_INVALID`
  - Condition: JSON / schema 검증 실패
  - Handling Layer: Callback or PortalCallbackPostProcessor
  - User Exposure: 4xx 응답, 상태 `FAILED_RESULT_SCHEMA`
- Error Code: `SCRAPE_RESULT_POST_PROCESSING_FAILED`
  - Condition: Portal sync / DB 반영 실패
  - Handling Layer: PortalCallbackPostProcessor
  - User Exposure: 5xx 응답, 상태 `FAILED_POST_PROCESSING`
- Error Code: `SCRAPE_STATE_CONFLICT`
  - Condition: terminal 상태 이후 중복 콜백으로 비정상 상태 전이 시도
  - Handling Layer: Application 서비스
  - User Exposure: 내부 로그

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [ ] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete
- [ ] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- Path: docs/specs/20260410-s3-callback/
  - Description: feat/215 Context Spec Bundle
  - Layer: Documentation
