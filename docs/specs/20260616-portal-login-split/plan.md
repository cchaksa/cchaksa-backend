# 포털 로그인 검증 분리 Plan

1. 현재 포털 연동 controller, DTO, docs, OpenAPI 테스트 구조를 확인한다.
2. `POST /portal/login` controller 테스트와 `PortalLoginService` 실패 테스트를 작성한다.
3. `PortalLoginVerificationTokenService`의 발급, 검증, 만료 실패 테스트를 작성한다.
4. `PortalLinkJobService`가 job 생성 전에 token을 검증하는 실패 테스트를 작성한다.
5. `PortalLoginVerifier`와 `PortalClient.validateLogin`을 유지해 스크래퍼 `/login` 호출 경계를 둔다.
6. `PortalLoginService`, `PortalLoginVerificationTokenService`, `PortalLoginController`를 추가한다.
7. `PortalLinkJobService`를 token 검증 기반으로 변경한다.
8. OpenAPI와 static API 문서를 갱신한다.
9. 관련 테스트를 통과시킨 뒤 전체 테스트를 실행한다.

## 검증 명령

- `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLoginServiceUnitTests`.
- `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLoginVerificationTokenServiceUnitTests`.
- `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalLinkJobServiceUnitTests`.
- `./gradlew test --tests com.chukchuk.haksa.infrastructure.portal.client.PortalClientTests`.
- `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLoginControllerApiIntegrationTest`.
- `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest`.
- `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest`.
- `./gradlew test --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest`.
- API와 공개 계약 변경이므로 최종적으로 `./gradlew test`를 실행한다.
