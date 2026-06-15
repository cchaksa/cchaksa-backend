# Clarify

## Open Questions
| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | 탈퇴/삭제 사용자 토큰을 어떤 에러 코드로 반환할 것인가. | User/Codex | Resolved |

## Decisions
| # | Decision | Reason | Date |
|---|----------|--------|------|
| 1 | invalid access token 계열은 secure endpoint에서 401로 반환한다. | 인증 정보가 유효하지 않은 상황이며 502는 gateway/upstream 실패 의미라 부적절하다. | 2026-06-14 |
| 2 | 탈퇴/삭제 사용자 토큰은 401 `TOKEN_INVALID`로 처리한다. | 인증 필터에서 principal 로딩에 실패한 케이스이므로 리소스 조회 404가 아니라 인증 실패로 보는 것이 일관적이다. | 2026-06-14 |
| 3 | 컨트롤러 테스트 대신 JWT 필터 또는 security chain 테스트로 회귀를 고정한다. | 기존 컨트롤러 테스트는 `addFilters = false`라 증상을 포착하지 못한다. | 2026-06-14 |

## Risks / Unknowns
- Item: 운영 502가 API 서버 500을 gateway가 502로 변환한 것인지 직접 로그는 아직 없다.
  - Impact: 로컬 테스트에서는 status가 500으로 보일 수 있으나, 근본은 필터 밖으로 인증 예외가 새는 것이다.
  - Mitigation: 필터 단위 테스트에서 응답 status/body가 401로 직접 작성되는지 검증한다.
- Item: `origin/dev` 기준 컨트롤러와 DTO가 로컬 `dev`보다 최신이다.
  - Impact: 작업 기준이 `origin/dev`여야 `/api/users/me`를 포함한 실제 대상과 맞는다.
  - Mitigation: `feat/261`을 `origin/dev`에서 생성했다.

## Follow-ups
- [ ] 운영 배포 후 gateway 5xx 지표에서 invalid token 케이스가 사라지는지 확인한다. (Owner)
