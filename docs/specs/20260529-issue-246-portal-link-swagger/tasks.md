# Tasks

## Checklist
- [x] origin/dev 기준 feat/246 worktree 생성.
- [x] 기존 포털 링크/API docs 관련 테스트 기준선 확인.
- [x] OpenAPI mismatch RED 테스트 추가.
- [x] 포털 링크 성공 응답 wrapper 추가.
- [x] Controller docs schema와 media type 수정.
- [x] 관련 테스트 실행.
- [x] 전체 테스트 실행.
- [x] 추가 OpenAPI contract RED 테스트 작성 및 실패 확인.
- [x] 내부 콜백, 보호 API 401, public security, health/sentry 문서 수정.
- [x] 추가 관련 테스트 실행.
- [ ] dev push 및 재배포.
- [ ] 배포 후 live Swagger 재검증.
- [ ] 커밋 생성.

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest.apiDocsRespondWithJsonBody --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 2 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --rerun-tasks --console=plain` | Failed as expected. Missing `application/json` and wrapper schema. | 2026-05-29 |
| 3 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 4 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest.apiDocsRespondWithJsonBody --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 5 | `./gradlew test --console=plain` | Passed | 2026-05-29 |
| 6 | `./gradlew test --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --rerun-tasks --console=plain` | Failed as expected. 4 tests failed on global security, callback schema, wildcard media type, and missing protected 401 docs. | 2026-05-29 |
| 7 | `./gradlew test --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 8 | `./gradlew test --tests com.chukchuk.haksa.global.config.OpenApiResponseContractTest --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest.apiDocsRespondWithJsonBody --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 9 | `./gradlew build --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 10 | `git diff --check` | Passed | 2026-05-29 |

## Notes
- Observation: live dev `/v3/api-docs`에서 `/portal/link` 202는 `AcceptedResponse`를 직접 참조하고 실제 body의 `SuccessResponse` wrapper와 불일치했다.
- Observation: live dev `/internal/scrape-results` 잘못된 서명은 401을 반환하지만 Swagger에는 401이 없고 200은 wrapper가 빠져 있다.
- Observation: 보호 API 다수는 실제 무인증 호출 시 401을 반환하지만 Swagger 응답 코드에 401이 없다.
