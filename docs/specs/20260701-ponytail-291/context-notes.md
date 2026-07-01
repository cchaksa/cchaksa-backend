# Issue 291 Context Notes

- Work starts from `feat/290`, not directly from `dev`, because the user requested #291 to build on #290.
- Existing unrelated dirty files were present before this work and must not be staged.
- #291 is broader than one cleanup pass, so any unclear or behavior-changing item should be left out instead of guessed.
- `PortalClient` only used `WebClient.block()`, so `RestTemplate` preserves the blocking behavior and lets us remove `spring-webflux`.
- Initial portal connection and refresh had duplicated `PortalStudentInfo` to `StudentInitializationDataType` plus `StudentInfo` mapping.
- The frontend uses `/v3/api-docs`, so Springdoc is the OpenAPI source of truth and the static `openapi.yaml` can be removed.
- `AcademicRecord` only needs `studentId`, sorted `semesters`, and `summary` for persistence mapping; `CourseEnrollments` and pass-through methods had no external references.
- Focused `PortalClientTest` passed after replacing WebClient with RestTemplate.
- `./gradlew test` passed after all #291 changes.
- Focused `OpenApiResponseContractTest` and full `./gradlew test` passed after removing the static OpenAPI YAML.
- Gemini's `RestTemplate` bean comment on PR #293 is covered by the existing `OIDCConfig.restTemplate` bean.
- Gemini's null-body and portal-data guard comments are addressed with boundary checks plus focused tests.
