# Plan

## Architecture / Layering
- Domain impact: 없음.
- Application orchestration: 없음.
- Infrastructure touchpoints: 없음.
- Global/config changes:
  - `JwtAuthenticationFilter`에서 JWT 파싱 실패, 사용자 로딩 실패를 `AuthenticationEntryPoint`로 정리한다.
  - `SecurityContextHolder`는 인증 실패 시 명시적으로 비운다.
  - 기존 `CustomAuthenticationEntryPoint` 응답 포맷과 에러 코드를 재사용한다.

## Data / Transactions
- Repositories touched: 없음.
- Transaction scope: 없음.
- Consistency expectations:
  - invalid token 요청 후 SecurityContext에 인증 객체가 남지 않는다.
  - 인증 실패는 컨트롤러로 진행하지 않는다.

## Testing Strategy
- Domain tests: 없음.
- Application tests: 없음.
- Integration/API tests:
  - `JwtAuthenticationFilter` 단위 테스트를 추가한다.
  - 만료 토큰, invalid JWT, 사용자 없음 토큰이 각각 401로 응답되는지 검증한다.
  - 사용자 없음 케이스는 loader가 `TokenException(USER_NOT_FOUND)`를 던지도록 구성한다.
- Additional commands:
  - `./gradlew test --tests com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilterTests`
  - `./gradlew test`

## Rollout Considerations
- Backward compatibility:
  - 5xx로 새던 인증 실패를 401로 바로잡는 변경이다.
  - JSON 에러 응답 포맷은 기존 `ErrorResponse`와 `CustomAuthenticationEntryPoint`를 유지한다.
- Observability / metrics:
  - invalid token 요청은 더 이상 서버 오류로 잡히지 않아야 한다.
- Feature flags / toggles: 없음.
