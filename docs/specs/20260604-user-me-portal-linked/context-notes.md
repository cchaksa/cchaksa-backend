# Context Notes

## 2026-06-04

- 작업 브랜치는 `feat/255`다.
- 브랜치는 최신 `origin/dev` 기준으로 만들었다.
- `/api/student/profile`은 학생 미연결 사용자에게 `U04 USER_NOT_CONNECTED`를 반환하므로, 미연동 여부 조회 API로는 적합하지 않다.
- `isPortalLinked`의 기존 의미는 로그인 응답의 포털 연동 여부와 동일하게 유지한다.
- 원천 값은 `users.portal_connected`이며, Java 값이 `null`이면 응답에서는 `false`로 취급한다.
- 서비스 RED는 `MeResponse`와 `getMe` 부재 컴파일 실패로 확인했다.
- 컨트롤러 RED는 `/api/users/me` 미구현으로 `NoResourceFoundException`이 `E-UNHANDLED` 500 응답으로 감싸지는 상태로 확인했다.
- OpenAPI RED는 `/api/users/me`의 `bearerAuth`, 401 응답, `MeApiResponse` wrapper 부재로 확인했다.
- 직접 관련 테스트와 전체 `./gradlew test`가 통과했다.
