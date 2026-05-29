# Context Notes

- 2026-05-29: 사용자 사진과 live dev `/v3/api-docs` 확인 결과, `/portal/link` 202 성공 응답이 실제로는 `SuccessResponse<AcceptedResponse>`인데 Swagger는 `AcceptedResponse`를 직접 참조한다.
- 2026-05-29: `/portal/link/jobs/{jobId}`와 `/portal/link/jobs/{jobId}/summary`도 각각 `JobStatusResponse`, `JobSummaryResponse`를 직접 참조하므로 같은 방식으로 수정한다.
- 2026-05-29: 실제 응답 body는 이미 원하는 구조이므로 컨트롤러 반환 로직은 바꾸지 않는다.
- 2026-05-29: `@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, ...)`를 포털 링크 docs에 명시해 Swagger UI의 media type도 `application/json`으로 보이게 한다.
