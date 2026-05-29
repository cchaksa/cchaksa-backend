# Spec Template

## 1. Feature Overview
- Purpose: 포털 LINK 콜백 후처리 중 같은 학번의 기존 연동 사용자가 현재 사용자로 병합되면 신규 초기화 대신 REFRESH 재동기화로 처리한다.
- Scope
  - In: `PortalSyncService.syncWithPortal`의 병합 후 분기, 해당 회귀 테스트, 작업 문서.
  - Out: 스크래퍼, SQS/S3 콜백 계약, 클라이언트 API 응답 계약, 기존 실패 job 재처리.
- Expected Impact: 최초 LINK 사용자는 기존 초기화 흐름을 유지하고, 기존 연동 계정 병합 케이스만 후처리 성공으로 전환한다.
- Stakeholder Confirmation: 2026-05-25 사용자 지시 “main 기준으로 feat/233 브랜치 만들어서 작업해.”를 구현 승인으로 기록한다.

## 2. Domain Rules
- Rule 1: LINK 작업은 생성 시점의 사용자 상태로 저장되지만, 콜백 후처리 시점에 같은 학번 기준 병합이 발생할 수 있다.
- Rule 2: 병합 후 active user가 이미 `portalConnected=true`라면 신규 포털 초기화가 아니라 REFRESH 재동기화로 처리한다.
- Rule 3: 병합 후에도 active user가 `portalConnected=false`라면 기존 신규 LINK 초기화 흐름을 유지한다.
- Mutable Rules: LINK 후처리에서 병합 후 상태에 따라 REFRESH 동기화 경로로 전환할 수 있다.
- Immutable Rules: 신규 최초 LINK의 초기화 동작, 외부 콜백/API 계약, scrape job 성공/실패 상태 모델은 바꾸지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 최초 포털 LINK
  - Trigger: `operationType=LINK` scrape job 성공 콜백 수신.
  - Actor: 백엔드 콜백 후처리.
  - Steps: `studentCode` 기준 기존 사용자가 없어서 현재 사용자를 유지하고 포털 초기화를 수행한다.
  - Expected Result: 기존처럼 포털 초기화, 학업 이력 동기화, 연결 마킹 후 scrape job이 성공한다.

- Scenario Name: 기존 연동 사용자 병합 LINK
  - Trigger: `operationType=LINK` scrape job 성공 콜백에서 같은 `studentCode`의 기존 연동 사용자가 발견된다.
  - Actor: 백엔드 콜백 후처리.
  - Steps: 기존 사용자를 현재 사용자로 병합하고, 병합 후 active user가 이미 연동 상태이면 REFRESH 재동기화 경로로 처리한다.
  - Expected Result: `already_connected`로 실패하지 않고 학업 이력 재동기화와 `lastSyncedAt` 갱신 후 scrape job이 성공한다.

### Exception / Boundary Flow
- Scenario Name: REFRESH 재동기화 실패
  - Condition: 병합 후 REFRESH 경로의 포털 연결 갱신 또는 학업 이력 재동기화가 실패한다.
  - Expected Behavior: 기존 REFRESH 실패 정책과 동일하게 `REFRESH_FAILED` 계열 예외로 실패를 기록한다.

## 4. Transaction / Consistency
- Transaction Start Point: `PortalSyncService.syncWithPortal` 또는 `refreshFromPortal` 진입.
- Transaction End Point: active user 저장, student reconnect 마킹, scrape job 성공 마킹이 포함된 콜백 후처리 트랜잭션 종료.
- Atomicity Scope: 사용자 병합, 학업 이력 동기화, user sync timestamp 갱신, scrape job 후처리 성공/실패 상태.
- Eventual Consistency Allowed: 외부 스크래퍼 결과 수신과 S3 payload 저장은 기존처럼 콜백 기반 비동기 흐름을 유지한다.

## 5. API List (필요 시)
- Endpoint: 변경 없음.
  - Method: 변경 없음.
  - Request DTO: 변경 없음.
  - Response DTO: 변경 없음.
  - Authorization: 변경 없음.
  - Idempotency: 기존 scrape job idempotency 유지.

## 6. Exception Policy
- Error Code: `SCRAPING_FAILED`
  - Condition: 최초 LINK 초기화 또는 학업 이력 동기화 실패.
  - Message Convention: 기존 메시지 유지.
  - Handling Layer: application 후처리.
  - User Exposure: 기존 API 에러 응답 정책 유지.

- Error Code: `REFRESH_FAILED`
  - Condition: 병합 후 이미 연동된 LINK를 REFRESH처럼 처리하는 중 연결 갱신 또는 재동기화 실패.
  - Message Convention: 기존 메시지 유지.
  - Handling Layer: application 후처리.
  - User Exposure: 기존 API 에러 응답 정책 유지.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/spec.md`
  - Description: 이슈 233 수정 범위와 정책.
  - Layer: Context
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/clarify.md`
  - Description: 결정사항과 위험.
  - Layer: Context
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/plan.md`
  - Description: 구현 및 검증 계획.
  - Layer: Context
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/tasks.md`
  - Description: 작업 체크리스트와 테스트 로그.
  - Layer: Context
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/checklist.md`
  - Description: 실행 체크리스트.
  - Layer: Context
- Path: `docs/specs/20260525-issue-233-link-merge-refresh/context-notes.md`
  - Description: 작업 중 판단 근거.
  - Layer: Context
