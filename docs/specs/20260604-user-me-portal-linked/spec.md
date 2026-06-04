# Spec

## 목적

프론트엔드가 포털 연동 여부를 임의로 추론하지 않도록, 로그인된 사용자의 계정 상태를 조회하는 API를 추가한다.

## 배경

현재 로그인 응답에는 `isPortalLinked`가 포함되어 있다. 하지만 앱 진입 후 최신 사용자 상태를 다시 확인할 수 있는 전용 API가 없다.

`/api/student/profile`은 학생 정보가 연결되지 않은 사용자에게 `U04 USER_NOT_CONNECTED` 에러를 반환한다. 따라서 해당 API의 성공 응답 body에 `isPortalLinked`를 추가해도 미연동 사용자는 값을 받을 수 없다.

## 범위

- `GET /api/users/me`를 추가한다.
- 응답은 `users.portal_connected`를 기준으로 `isPortalLinked`를 반환한다.
- `portal_connected`가 `null`이면 `false`로 반환한다.
- bearer 인증이 필요한 사용자 API로 문서화한다.
- 정적 OpenAPI 문서와 springdoc wrapper를 함께 갱신한다.

## 비범위

- `/api/student/profile` 응답 구조는 변경하지 않는다.
- 로그인 응답의 `isPortalLinked` 의미는 변경하지 않는다.
- 포털 연동 로직이나 병합 로직은 변경하지 않는다.
