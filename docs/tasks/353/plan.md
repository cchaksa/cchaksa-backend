# 테스트 데이터 API 경계 분리 계획

## 1. 패키지와 프로파일

- `domain/admin`의 테스트 지원 Controller, docs, DTO, Service를 `domain/testsupport`로 이동하고 `AdminTest*` 이름을 `Test*`로 정리한다.
- Controller와 네 Service에 `@Profile({"dev", "test"})`를 적용한다.
- 단위·통합 테스트의 package와 import를 함께 갱신한다.

## 2. API와 보안

- Controller 기본 경로와 하위 경로를 `design.md` 계약대로 `/api/test/**`로 변경한다.
- `SecurityConfig.PUBLIC_ENDPOINTS`에서 `/api/admin/**` 테스트 matcher를 제거하고 공개 `/api/test/**` matcher를 추가한다.
- `/api/test/me/**`는 공개 목록에 넣지 않아 JWT 인증을 유지한다.

## 3. 호출부와 계약 테스트

- `AdminTestControllerApiIntegrationTest`, `AdminTransferDataHttpIntegrationTest`, `JwtAuthenticationFilterTests`의 URL을 갱신한다.
- `OpenApiResponseContractTest`의 public/protected operation 경로를 변경한다.
- prod 프로파일 테스트를 Controller와 네 Service 빈 부재 검증으로 확장한다.
- 과거 기록인 `docs/specs/**`는 수정하지 않는다.

## 4. 검증과 커밋

- `./gradlew spotlessApply --no-daemon`.
- `./gradlew test --tests '*TestData*' --tests '*TestAccount*' --tests '*TestOption*' --tests '*TestMutation*' --tests '*TestLectureEvaluation*' --tests '*JwtAuthenticationFilterTests' --tests '*OpenApiResponseContractTest' --stacktrace --no-daemon`.
- `./gradlew check --stacktrace --no-daemon`.
- `git diff --check`와 prod 프로파일·OpenAPI 경계를 검토한다.
- `353 feat: 테스트 데이터 API 경계를 분리`로 커밋한다.

## Wiki

개발 테스트 API 경로와 운영 부재 조건이 바뀌므로 관련 Wiki 갱신이 필요하다. 이 저장소 커밋에는 Wiki 패치를 포함하지 않고 최종 결과에 갱신 필요를 기록한다.
