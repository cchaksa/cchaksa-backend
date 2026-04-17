# Spec

## 1. Feature Overview
- Purpose: Lambda 환경에서 `/portal/link` 이후 SQS 발행이 유실되어 `scrape_jobs.status=QUEUED`에 고착되는 문제를 제거한다.
- Scope
  - In:
    - `/portal/link` 요청 수락 직후 SQS 발행 경계 재구성
    - `after_commit + TaskExecutor/@Scheduled` 기반 outbox dispatch 제거
    - 기존 API Lambda 안에서 동기 SQS 발행 및 실패 처리 정리
    - outbox/job 상태 전이와 관련 로그/테스트 보강
  - Out:
    - `/internal/scrape-results` callback 계약 변경
    - worker/S3 처리 로직 변경
    - 별도 Dispatcher Lambda 추가
- Expected Impact:
  - `accepted` 이후 SQS 발행 성공/실패가 요청 안에서 결정된다.
  - Lambda in-memory 비동기 의존이 제거된다.
  - `QUEUED` 고착과 내부 dispatch 유실 가능성이 줄어든다.
- Stakeholder Confirmation: 2026-04-17 사용자 지시 "기존 Lambda에서 동기 SQS 발행하도록 작업 시작해"를 구현 승인으로 기록한다.

## 2. Domain Rules
- Rule 1: `scrape_jobs`와 `scrape_job_outbox`는 동일 트랜잭션에서 저장되어야 한다.
- Rule 2: SQS 발행 성공 전 job 상태는 `QUEUED`를 유지하고, 성공 시에만 `RUNNING`으로 전이한다.
- Rule 3: SQS 발행 실패는 응답/로그/상태에 반영되어 운영자가 즉시 인지할 수 있어야 한다.
- Mutable Rules:
  - `/portal/link` 내부 dispatch 구현 방식
  - outbox 재시도/스케줄링 구성
- Immutable Rules:
  - `/portal/link`의 public API 계약과 idempotency 의미
  - `/internal/scrape-results`의 callback 처리 의미

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 링크 요청 수락 후 동기 enqueue 성공
  - Trigger: 인증된 사용자가 `/portal/link`를 호출한다.
  - Actor: Backend API Lambda
  - Steps:
    1. 요청과 idempotency key를 검증한다.
    2. `scrape_jobs`와 `scrape_job_outbox`를 트랜잭션으로 저장한다.
    3. 트랜잭션 종료 후 같은 요청 흐름에서 SQS `SendMessage`를 동기 호출한다.
    4. SQS 발행 성공 시 outbox를 `SENT`, job을 `RUNNING`으로 갱신한다.
    5. `202 Accepted`를 반환한다.
  - Expected Result: worker가 시작 가능한 메시지가 실제로 큐에 존재하고, job 상태가 `RUNNING`으로 전이된다.

### Exception / Boundary Flow
- Scenario Name: 동기 enqueue 실패
  - Condition: SQS 발행 또는 publish 후 상태 반영 중 예외가 발생한다.
  - Expected Behavior:
    - 요청은 성공으로 응답하지 않는다.
    - outbox와 job 상태에 실패 원인이 남는다.
    - Lambda 내부 background scheduler에 재시도를 맡기지 않는다.

- Scenario Name: 동일 idempotency key 재요청
  - Condition: 이미 같은 fingerprint/job이 존재한다.
  - Expected Behavior: 기존 job을 재사용하고 새 outbox/job을 중복 생성하지 않는다.

## 4. Transaction / Consistency
- Transaction Start Point: `PortalLinkJobService.acceptJob()`에서 job/outbox 저장 시점
- Transaction End Point: job/outbox 저장 commit 시점, 이후 SQS 발행 결과 반영용 별도 짧은 트랜잭션
- Atomicity Scope: `scrape_jobs` + `scrape_job_outbox` 저장은 원자적이어야 한다.
- Eventual Consistency Allowed: commit 이후 SQS 발행/상태 반영은 별도 짧은 단계로 분리되나, 요청 종료 전에 성공/실패를 확정한다.

## 5. API List (필요 시)
- Endpoint: `/portal/link`
  - Method: `POST`
  - Request DTO: `PortalLinkDto.LinkRequest`
  - Response DTO: `PortalLinkDto.AcceptedResponse`
  - Authorization: 인증 사용자 필요
  - Idempotency: `Idempotency-Key` 헤더 유지

## 6. Exception Policy
- Error Code: `SCRAPE_JOB_ENQUEUE_FAILED`
  - Condition: SQS 발행 실패 또는 publish 결과 반영 실패
  - Message Convention: enqueue 경계에서 실패 원인을 요약해 로그/상태에 남긴다.
  - Handling Layer: application + infrastructure
  - User Exposure: 기존 enqueue 실패 응답 정책 유지
- Error Code: `IDEMPOTENCY_KEY_CONFLICT`
  - Condition: 같은 key에 다른 fingerprint 요청이 들어온다.
  - Message Convention: 기존 정책 유지
  - Handling Layer: application
  - User Exposure: 기존 정책 유지

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `docs/specs/20260417-issue-218-sync-sqs-dispatch/spec.md`
  - Description: 동기 SQS 발행 전환 스펙
  - Layer: spec
- Path: `docs/specs/20260417-issue-218-sync-sqs-dispatch/clarify.md`
  - Description: 의사결정/리스크 정리
  - Layer: spec
- Path: `docs/specs/20260417-issue-218-sync-sqs-dispatch/plan.md`
  - Description: 레이어별 구현 계획
  - Layer: spec
- Path: `docs/specs/20260417-issue-218-sync-sqs-dispatch/tasks.md`
  - Description: 작업/테스트 체크리스트
  - Layer: spec
