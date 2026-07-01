# Issue 291 Context Notes

- Work starts from `feat/290`, not directly from `dev`, because the user requested #291 to build on #290.
- Existing unrelated dirty files were present before this work and must not be staged.
- #291 is broader than one cleanup pass, so any unclear or behavior-changing item should be left out instead of guessed.
- `PortalClient` only used `WebClient.block()`, so `RestTemplate` preserves the blocking behavior and lets us remove `spring-webflux`.
- Initial portal connection and refresh had duplicated `PortalStudentInfo` to `StudentInitializationDataType` plus `StudentInfo` mapping.
- OpenAPI source-of-truth cleanup is intentionally skipped in this pass because changing that policy is not a small mechanical cleanup.
- `AcademicRecord` only needs `studentId`, sorted `semesters`, and `summary` for persistence mapping; `CourseEnrollments` and pass-through methods had no external references.
- Focused `PortalClientTest` passed after replacing WebClient with RestTemplate.
- `./gradlew test` passed after all #291 changes.
