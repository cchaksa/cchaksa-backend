# 221 EventBridge Maintenance Direct Invoke

## 1. Feature Overview
- Purpose: Lambda 내부 `@Scheduled` 실행에 의존하던 유지보수 작업을 EventBridge Scheduler가 백엔드 Lambda를 직접 invoke하는 방식으로 전환한다.
- Scope
  - In:
    - EventBridge Scheduler payload를 Lambda handler에서 감지하고 HTTP proxy 처리와 분기한다.
    - `SCRAPE_JOB_RECONCILE_STALE`, `REFRESH_TOKEN_CLEANUP` maintenance task를 처리한다.
    - 기존 stale scrape job 정리와 refresh token 정리 로직은 재사용하되 `@Scheduled` 의존을 제거한다.
  - Out:
    - HTTP internal endpoint 추가.
    - HMAC 인증 추가.
    - Terraform/EventBridge 리소스 생성.
    - 신규 DB DDL.
- Expected Impact:
  - Lambda 런타임 내부 scheduler/thread 생명주기에 의존하지 않고 외부 durable trigger로 유지보수 작업을 실행할 수 있다.
  - 기존 HTTP API Lambda handler는 HTTP 요청을 계속 처리한다.
- Stakeholder Confirmation:
  - 2026-04-26 사용자 요청: `feat/218` 기준 `feat/221` 생성 후 EventBridge Scheduler 직접 invoke 방식 적용.

## 2. Domain Rules
- Rule 1: stale scrape job 정리는 기존 정책을 유지한다. 오래 `RUNNING`에 머문 job은 `FAILED`와 callback timeout 계열 오류로 전이한다.
- Rule 2: 만료된 refresh token 삭제는 기존 삭제 기준을 유지한다.
- Rule 3: maintenance task는 허용된 task enum만 실행하며, 알 수 없는 task는 실패로 처리한다.
- Mutable Rules:
  - EventBridge payload의 `scheduled_at`은 로깅/추적용이며 비즈니스 기준 시간으로 사용하지 않는다.
- Immutable Rules:
  - EventBridge Scheduler 인증은 HTTP/HMAC이 아니라 Lambda invoke IAM 권한으로 처리한다.
  - Lambda handler는 HTTP API event를 기존 proxy 경로로 계속 전달해야 한다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: stale scrape job reconcile
  - Trigger: EventBridge Scheduler 직접 Lambda invoke
  - Actor: EventBridge Scheduler role
  - Steps:
    1. Lambda handler가 payload `source=eventbridge.scheduler`, `task=SCRAPE_JOB_RECONCILE_STALE`를 감지한다.
    2. Spring bean maintenance handler로 라우팅한다.
    3. stale job reconciler가 기존 정책대로 오래된 job을 실패 처리한다.
  - Expected Result: 성공 응답 JSON에 task, success, affected count가 기록된다.

- Scenario Name: refresh token cleanup
  - Trigger: EventBridge Scheduler 직접 Lambda invoke
  - Actor: EventBridge Scheduler role
  - Steps:
    1. Lambda handler가 payload `task=REFRESH_TOKEN_CLEANUP`를 감지한다.
    2. refresh token cleanup service를 호출한다.
  - Expected Result: 성공 응답 JSON에 task, success, deleted count가 기록된다.

### Exception / Boundary Flow
- Scenario Name: unknown maintenance task
  - Condition: `source=eventbridge.scheduler`이지만 task가 허용 목록에 없음
  - Expected Behavior: Lambda invocation이 실패하고 error log를 남긴다.

- Scenario Name: HTTP request
  - Condition: HTTP API v2 payload가 들어옴
  - Expected Behavior: 기존 `SpringBootLambdaContainerHandler.proxyStream` 경로로 처리한다.

## 4. Transaction / Consistency
- Transaction Start Point:
  - stale job reconcile: 기존 reconciler 트랜잭션 경계.
  - refresh token cleanup: refresh token 삭제 메서드 트랜잭션 경계.
- Transaction End Point: 각 maintenance task 메서드 종료 시점.
- Atomicity Scope: task 1회 실행 단위.
- Eventual Consistency Allowed: EventBridge Scheduler 재시도에 의해 동일 작업이 다시 실행될 수 있으므로 cleanup/reconcile은 idempotent해야 한다.

## 5. API List
- Public HTTP Endpoint: 없음.
- Lambda Event Contract:
  - Request:
    - `source`: `eventbridge.scheduler`
    - `task`: `SCRAPE_JOB_RECONCILE_STALE` 또는 `REFRESH_TOKEN_CLEANUP`
    - `scheduled_at`: ISO-8601 문자열, optional
  - Response:
    - `success`: boolean
    - `task`: string
    - `affected_count`: number
    - `scheduled_at`: string/null

## 6. Exception Policy
- Error Code: `UNKNOWN_MAINTENANCE_TASK`
  - Condition: task 값이 없거나 허용 목록에 없음
  - Message Convention: task/source 값을 포함해 운영 로그로 추적 가능하게 작성
  - Handling Layer: global lambda maintenance handler
  - User Exposure: EventBridge invocation failure
- Error Code: `MAINTENANCE_TASK_FAILED`
  - Condition: task 실행 중 예외 발생
  - Message Convention: task, scheduled_at, exception class를 로그에 남김
  - Handling Layer: application/global
  - User Exposure: EventBridge invocation failure

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `src/main/java/com/chukchuk/haksa/application/maintenance`
  - Description: maintenance task routing and service orchestration
  - Layer: Application
- Path: `src/main/java/com/chukchuk/haksa/global/lambda`
  - Description: EventBridge direct invoke detection and routing
  - Layer: Global
