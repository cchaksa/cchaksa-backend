# Context Notes

- 2026-05-29: 사용자 사진과 live dev `/v3/api-docs` 확인 결과, `/portal/link` 202 성공 응답이 실제로는 `SuccessResponse<AcceptedResponse>`인데 Swagger는 `AcceptedResponse`를 직접 참조한다.
- 2026-05-29: `/portal/link/jobs/{jobId}`와 `/portal/link/jobs/{jobId}/summary`도 각각 `JobStatusResponse`, `JobSummaryResponse`를 직접 참조하므로 같은 방식으로 수정한다.
- 2026-05-29: 실제 응답 body는 이미 원하는 구조이므로 컨트롤러 반환 로직은 바꾸지 않는다.
- 2026-05-29: `@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, ...)`를 포털 링크 docs에 명시해 Swagger UI의 media type도 `application/json`으로 보이게 한다.
- 2026-05-29: 추가 live 검증 결과 `/internal/scrape-results` 서명 실패는 실제 401이며, Swagger에는 401이 누락되어 있다.
- 2026-05-29: `SecurityConfig`의 public endpoint는 실제로 인증 없이 접근되지만 OpenAPI 전역 security 때문에 Swagger에서 공개 API까지 bearerAuth를 상속받는다. 전역 security를 제거하고 보호 API의 operation-level security를 source of truth로 둔다.
- 2026-05-29: 보호 API의 공통 인증 실패는 Spring Security entry point에서 401 error wrapper로 내려가므로, operation security가 있는 API에는 문서에도 401을 포함한다.
