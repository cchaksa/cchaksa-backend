# Context Notes

## 2026-06-14

- 사용자는 이슈 번호 261을 제공했고 즉시 작업 시작을 지시했다.
- 현재 작업 브랜치는 `feat/261`이다.
- 기존 워크트리에는 사용자 변경으로 보이는 `.codex/config.toml` 수정과 `docs/lambda-migration-backend-lessons.md` 미추적 파일이 있었다. 이번 작업에서는 건드리지 않는다.
- 로컬 `dev`는 `origin/dev`보다 뒤처져 있었고, `GET /api/users/me`는 `origin/dev` 기준 코드에 존재한다. 작업 브랜치는 `origin/dev` 기반으로 생성했다.
- 원인 가설은 `JwtAuthenticationFilter`에서 인증 실패 예외가 `CustomAuthenticationEntryPoint`의 401 응답 작성으로 정리되지 않고 필터 밖으로 새는 것이다.
- 탈퇴/삭제 사용자 토큰은 컨트롤러 이후의 리소스 조회 실패가 아니라 인증 단계의 principal 로딩 실패이므로 401 `TOKEN_INVALID`로 처리한다.

## 2026-06-15

- PR #262 리뷰에서 JWT subject가 없을 때 `NullPointerException`이 필터 밖으로 새어 다시 500/502가 될 수 있다는 지적이 있었다.
- `NullPointerException` catch 추가보다 subject 값을 명시적으로 검증하고 `JwtException`으로 invalid token 처리하는 쪽이 더 좁고 명확하다.
- subject 누락 access token은 401 `TOKEN_INVALID`로 처리한다.
