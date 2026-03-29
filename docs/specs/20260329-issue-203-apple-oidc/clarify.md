# Clarify Template

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 허용 client id 목록은 어떤 값으로 시작해야 하는가? | Codex | 사용자 요청에 명시된 `com.cchaksa.app`, `com.chukchukhaksa.moblie.ChukChukHaksa` 2개 + fallback client id 유지로 확정 |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | 기존 `security.apple.client-id` 설정은 token exchange/revoke 용도로 그대로 유지하고, 허용 목록이 비어 있으면 fallback으로 사용한다. | 기존 인프라 호환 보장 및 설정 누락 시 안전장치 | 2026-03-29 |
| 2 | 허용 목록 설정은 comma-separated string을 `security.apple.allowed-client-ids`로 정의하고, env에서는 `APPLE_ALLOWED_CLIENT_IDS`를 사용한다. | 배포 환경에서 여러 값을 간단히 주입하기 위함 | 2026-03-29 |
| 3 | 사용자 요청(ISSUE-203)에 따라 본 스펙에 대해 \"OK to implement\" 승인 완료 | 명시적 승인 요건 충족 | 2026-03-29 |

## Risks / Unknowns
- Item: 허용 목록 업데이트가 운영 환경에 즉시 반영되지 않을 수 있음
  - Impact: 신규 client id 추가 시에도 서버 설정 미반영이면 여전히 로그인 실패 발생
  - Mitigation: DevOps에 새 env var 적용 절차를 공유하고, `tasks.md`에 follow-up 기록

## Follow-ups
- [ ] 운영/배포 환경에 `APPLE_ALLOWED_CLIENT_IDS` 값 반영 안내 (Owner: Ops)
