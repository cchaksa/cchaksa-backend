# Tasks

## Checklist
- [x] origin/dev 기준 feat/246 worktree 생성.
- [x] 기존 포털 링크/API docs 관련 테스트 기준선 확인.
- [x] OpenAPI mismatch RED 테스트 추가.
- [x] 포털 링크 성공 응답 wrapper 추가.
- [x] Controller docs schema와 media type 수정.
- [x] 관련 테스트 실행.
- [x] 전체 테스트 실행.
- [ ] 커밋 생성.

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest.apiDocsRespondWithJsonBody --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 2 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --rerun-tasks --console=plain` | Failed as expected. Missing `application/json` and wrapper schema. | 2026-05-29 |
| 3 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 4 | `./gradlew test --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkOpenApiTest --tests com.chukchuk.haksa.domain.portal.controller.PortalLinkControllerApiIntegrationTest --tests com.chukchuk.haksa.domain.portal.controller.PortalJobQueryControllerApiIntegrationTest --tests com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest.apiDocsRespondWithJsonBody --rerun-tasks --console=plain` | Passed | 2026-05-29 |
| 5 | `./gradlew test --console=plain` | Passed | 2026-05-29 |

## Notes
- Observation: live dev `/v3/api-docs`에서 `/portal/link` 202는 `AcceptedResponse`를 직접 참조하고 실제 body의 `SuccessResponse` wrapper와 불일치했다.
