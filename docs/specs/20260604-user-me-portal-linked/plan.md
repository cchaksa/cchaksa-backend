# Plan

## 구현 계획

1. 사용자 상태 응답 DTO를 추가한다.
   - `UserDto.MeResponse`에 `boolean isPortalLinked`를 둔다.

2. 사용자 상태 조회 서비스 메서드를 추가한다.
   - `UserService.getMe(UUID userId)`가 사용자를 조회한다.
   - 사용자가 없으면 기존 `USER_NOT_FOUND` 예외를 사용한다.
   - `portalConnected`가 `Boolean.TRUE`일 때만 `true`를 반환한다.

3. 사용자 컨트롤러에 `GET /api/users/me`를 추가한다.
   - `@AuthenticationPrincipal CustomUserDetails`에서 사용자 ID를 가져온다.
   - `SuccessResponse.of(response)` 형태를 유지한다.

4. OpenAPI 문서를 갱신한다.
   - springdoc 인터페이스에 operation을 추가한다.
   - 정적 `src/main/resources/public/openapi.yaml`에 path와 schema를 추가한다.
   - `OpenApiResponseContractTest`의 wrapper ref 기대값을 추가한다.

5. 테스트를 추가하고 검증한다.
   - 컨트롤러 테스트는 `isPortalLinked`가 200 body에 내려가는지 확인한다.
   - 서비스 테스트는 `true`, `false/null`, 사용자 없음을 확인한다.
   - 관련 테스트와 전체 테스트를 실행한다.
