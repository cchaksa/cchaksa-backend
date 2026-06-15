# Tasks

## Checklist
- [x] Phase 0 intent routing: `BUG_FIX.md`.
- [x] Phase 1 Spec Bundle 생성.
- [x] Clarify 결정 기록.
- [x] Plan 작성.
- [x] JWT 필터 회귀 테스트 작성.
- [x] RED 테스트 실행 결과 기록.
- [x] Global/security 수정.
- [x] GREEN targeted test 실행 결과 기록.
- [x] `./gradlew test` 실행 결과 기록.
- [x] 커밋 생성.

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests` | Failed as expected. 3 tests failed because authentication exceptions escaped the filter instead of returning 401. | 2026-06-14 |
| 2 | `./gradlew test --tests com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests` | Passed. | 2026-06-14 |
| 3 | `./gradlew test` | Passed. | 2026-06-14 |
| 4 | `./gradlew build` | Passed. | 2026-06-14 |
| 5 | `./gradlew test --tests com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests` | Failed as expected. Missing subject token raised `NullPointerException` before fix. | 2026-06-15 |
| 6 | `./gradlew test --tests com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests` | Passed. | 2026-06-15 |
| 7 | `./gradlew test` | Passed. | 2026-06-15 |
| 8 | `./gradlew build` | Passed. | 2026-06-15 |

## Notes
- Observation: 기존 컨트롤러 WebMvc 테스트는 `addFilters = false`라 JWT 인증 필터 실패를 검증하지 못한다.
- Observation: `JwtAuthenticationFilter`는 `ExpiredJwtException`, `JwtException`, `IllegalStateException`만 잡고 `TokenException`은 잡지 않는다.
- Observation: 필터에서 던진 인증 예외가 entry point 응답으로 정리되지 않으면 API 서버 500 또는 gateway 502로 관측될 수 있다.
- Observation: subject가 없는 JWT도 invalid token으로 분류해 401 `TOKEN_INVALID`를 반환해야 한다.
