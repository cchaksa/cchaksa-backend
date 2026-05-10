# Clarifications

> Legacy context migrated on 2026-04-16 from `docs/context/20260223-auth-studentid-separation.md`.

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 없음. 구현 범위와 예외 의미는 사용자 승인된 계획으로 잠금됨 | requester / codex | Closed |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 이번 이슈는 `/api/student/profile`만 `userId` 기준 단일 조회로 전환한다 | legacy context의 인증/도메인 분리 원칙을 유지하면서 범위를 최소화하기 위해 | 2026-04-16 |
| 2 | `USER_NOT_FOUND`와 `USER_NOT_CONNECTED`의 의미를 기존대로 유지한다 | 외부 contract와 운영 해석을 바꾸지 않기 위해 | 2026-04-16 |
| 3 | 사용자 메시지 “PLEASE IMPLEMENT THIS PLAN”을 Phase 2 진입 승인으로 기록한다 | Spec Bundle 승인 근거를 문서에 남기기 위해 | 2026-04-16 |
| 4 | 구현 후에도 커밋은 사용자 확인 전까지 하지 않는다 | 요청자가 명시적으로 선커밋 금지를 요청함 | 2026-04-16 |

## Risks / Unknowns
- Item:
  - Impact:
    - `User` 루트 fetch query가 `StudentRepository` 기반 기존 조회와 다른 연관 로딩 누락을 만들 수 있다.
  - Mitigation:
    - `department`, `major`, `secondaryMajor`, `user`를 모두 테스트로 검증한다.
- Item:
  - Impact:
    - Swagger 문서가 실제 예외 의미와 어긋나 있으면 QA와 Postman 테스트가 혼동될 수 있다.
  - Mitigation:
    - `StudentControllerDocs`와 wrapper 예시값을 함께 수정한다.

## Follow-ups
- [x] 테스트 실행 결과를 `tasks.md`에 기록한다. (Codex)
- [ ] 사용자 검토 전까지 커밋하지 않는다. (Codex)
