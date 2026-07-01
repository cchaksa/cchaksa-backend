# Issue 291 Checklist

- [x] Create `feat/291` from `feat/290`.
- [x] Inspect `PortalClient` and HTTP client configuration.
- [x] Inspect portal initialization and refresh mapping duplication.
- [x] Inspect OpenAPI source-of-truth candidates.
- [x] Remove static `openapi.yaml` and keep `/v3/api-docs` as the only source of truth.
- [x] Inspect `AcademicRecord` and related helper references.
- [x] Apply only safe simplifications.
- [x] Run focused reference checks.
- [x] Run `./gradlew test`.
- [x] Commit in meaningful units.
