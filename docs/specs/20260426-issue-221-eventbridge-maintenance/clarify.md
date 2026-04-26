# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | EventBridge Scheduler는 HTTP endpoint를 호출할지 Lambda를 직접 invoke할지? | BE 팀 | Decided |
| 2 | Lambda direct invoke 인증은 어떻게 처리할지? | BE 팀 | Decided |
| 3 | 기존 내부 `@Scheduled` 유지 여부는? | BE 팀 | Decided |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | EventBridge Scheduler -> Backend Lambda direct invoke를 사용한다. | Lambda 환경에서 HTTP/HMAC endpoint보다 IAM invoke permission이 단순하고 안전하다. | 2026-04-26 |
| 2 | 인증은 HMAC이 아니라 scheduler role의 Lambda invoke IAM 권한으로 처리한다. | direct invoke는 API Gateway를 거치지 않으므로 IAM 경계가 자연스럽다. | 2026-04-26 |
| 3 | stale job reconcile과 refresh token cleanup은 내부 `@Scheduled`를 제거하고 maintenance event에서 직접 호출한다. | Lambda 내부 scheduler/thread 생명주기에 의존하지 않기 위함이다. | 2026-04-26 |
| 4 | HTTP internal endpoint는 추가하지 않는다. | 공격면과 인증 구현을 늘리지 않기 위함이다. | 2026-04-26 |
| 5 | 사용자 "feat/218을 기준으로 feat/221을 생성하고 아까 계획한 작업을 시작" 지시로 OK to implement 승인 확보. | Phase 2 진행 승인. | 2026-04-26 |

## Risks / Unknowns
- Item: Lambda handler에서 HTTP event와 maintenance event를 잘못 구분할 가능성
  - Impact: HTTP 요청이 maintenance 경로로 라우팅되거나 반대로 maintenance event가 HTTP proxy로 넘어갈 수 있음
  - Mitigation: `source=eventbridge.scheduler`와 `task` 존재 여부를 모두 확인하고, HTTP event 회귀 테스트 추가
- Item: EventBridge Scheduler 재시도에 따른 중복 실행
  - Impact: 동일 cleanup/reconcile이 반복 실행될 수 있음
  - Mitigation: 기존 cleanup/reconcile 로직을 idempotent하게 유지하고 affected count만 반환

## Follow-ups
- [ ] Terraform/EventBridge Scheduler rule, target, IAM invoke permission은 인프라 작업에서 반영한다.
