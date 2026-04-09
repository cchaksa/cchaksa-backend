# Portal Link Outbox Dispatch 안정화 스펙

## 1. Feature Overview
- Purpose: `/portal/link` 요청으로 생성된 스크레이핑 잡이 Queue 상태에 고립되지 않도록 after-commit outbox 디스패치를 안정화하고 shadow/dev/prod 환경 전체에서 SQS 전달을 보장한다.
- Scope
  - In: PortalLinkJobService 트랜잭션 경계 점검, ScrapeJobOutboxDispatcher afterCommit/커넥션 사용 방식 개선, 개발/운영 프로필 hikari 및 open-in-view 설정 검토, shadow 환경 스케줄러 비활성화 시 outbox 처리 경로 보강, 관측 gauge 커넥션 사용 최소화.
  - Out: 신규 엔드포인트 추가, 스크레이퍼 비즈니스 규칙 변경, 프런트엔드 변경.
- Expected Impact: develop-shadow에서 발생한 `Queue` 고정 장애 제거, dev/prod 환경에서도 동일 패턴으로 인한 잠재적 디스패치 실패 예방, SQS 릴레이 신뢰도 향상.
- Stakeholder Confirmation: develop-shadow 장애 리포트를 기준으로 FE/운영 팀 구두 OK, 구현 전 QA 공유 예정.

## 2. Domain Rules
- Rule 1: Portal link 잡은 accepted 상태에서 outbox 메시지가 성공적으로 발행되어야 상태 전이가 진행된다.
- Rule 2: Outbox 디스패치는 동일 트랜잭션 커밋 이후에만 실행되어 DB 레코드가 확정된 상태여야 한다.
- Rule 3: 동일 job의 중복 디스패치는 idempotent 해야 하며 실패 시 재시도 경로가 존재해야 한다.
- Rule 4: Worker 성공 콜백이 저장되더라도 Job 상태는 포털 초기화/학업 동기화가 모두 끝날 때까지 `SUCCEEDED` 로 전환되지 않는다. 후처리가 실패하면 Job 은 `FAILED` 로 롤백된다.
- Rule 5: 동일 학번(StudentCode)에 대해 단 하나의 Student 엔티티만 존재해야 하며 재시도 시 중복 레코드가 남지 않아야 한다.
- Mutable Rules: 디스패치 재시도 전략, gauge 수집 주기.
- Immutable Rules: 잡 상태 전이 규칙, outbox -> SQS contract.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: Portal link 잡 접수 및 디스패치
  - Trigger: 사용자가 `/portal/link` POST 호출
  - Actor: PortalLinkJobService, ScrapeJobOutboxDispatcher
  - Steps:
    1. PortalLinkJobService.acceptJob 가 job 엔터티를 Queue 상태로 저장한다.
    2. 트랜잭션 커밋 후 afterCommit hook이 ScrapeJobOutboxDispatcher.dispatchOnce 를 호출한다.
    3. Dispatcher가 outbox 테이블을 조회하여 메시지를 SQS `*-scraper-jobs` 로 보낸다.
    4. Job 상태가 Accepted/Dispatched 등 다음 단계로 전환된다.
  - Expected Result: 요청은 202 Accepted, 후속 비동기 처리 성공.

### Exception / Boundary Flow
- Scenario Name: 커넥션 고갈 등으로 afterCommit 실패
  - Condition: 동일 스레드에서 REQUIRES_NEW 트랜잭션 시작 시 Hikari pool에 사용 가능한 커넥션이 없음
  - Expected Behavior: 디스패치 스케줄러 또는 백오프 재시도가 커넥션 회복 후 다시 처리, shadow 환경에도 재시도 경로 존재.
- Scenario Name: 포털 콜백 후처리 실패 또는 Student 중복
  - Condition: `/internal/scrape-results` 콜백 이후 포털 초기화/학업 동기화 중 예외 발생 혹은 동일 학번 Student 중복으로 unique 제약 위반
  - Expected Behavior: Job 상태는 이미 `SUCCEEDED` 이고 그대로 유지되며, 후처리 실패 원인만 로그/메트릭으로 노출되고 Student/수강 데이터는 롤백되어 재시도 시 중복이 발생하지 않는다.

## 4. Transaction / Consistency
- Transaction Start Point: PortalLinkJobService.acceptJob 진입 시 @Transactional
- Transaction End Point: job insert/상태 변경 커밋 완료 시점
- Atomicity Scope: Job 생성 및 outbox 레코드 생성까지 단일 트랜잭션, 콜백 후처리(포털 초기화+학업 동기화+최종 상태 반영)는 AFTER_COMMIT 이벤트 안의 단일 REQUIRES_NEW 트랜잭션에서 원자적으로 수행하며 성공 시에만 `SUCCEEDED` 로 커밋된다. 후처리 실패 시 동일 트랜잭션에서 `FAILED` 로 마킹한다.
- Eventual Consistency Allowed: SQS 디스패치는 eventual 허용, 단 재시도 메커니즘 필수

## 5. API List (필요 시)
- Endpoint: `/portal/link`
  - Method: POST
  - Request DTO: PortalLinkJobRequest (기존)
  - Response DTO: 202 Accepted with tracking id
  - Authorization: JWT
  - Idempotency: 클라이언트 별 요청 키로 보장 (현행 규칙 유지)

## 6. Exception Policy
- Error Code: `PORTAL_LINK_DISPATCH_FAILED`
  - Condition: afterCommit 디스패치 경로에서 치명적 실패 시 (ex. 재시도 불가)
  - Message Convention: `portal_link.dispatch.error`
  - Handling Layer: ScrapeJobOutboxDispatcher + Global exception mapper
  - User Exposure: 로그/모니터링 기록, API 응답은 여전히 202
- Error Code: `SCRAPE_JOB_FAILED_RESULT` / `SCRAPE_JOB_NOT_COMPLETED` / `SCRAPE_JOB_NOT_FOUND`
  - Condition: `/portal/link/jobs/{jobId}` 등에서 Polling 중 발생하는 정상적인 비즈니스 예외
  - Handling Layer: `PortalLinkJobQueryService` → `GlobalExceptionHandler`
  - Sentry 정책: 사용자 행동 기반 4xx 잡음 방지를 위해 Sentry 이벤트를 남기지 않는다 (응답/로그만 남김)

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [ ] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete
- [ ] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- Path:
  - Description:
  - Layer:
