# Tasks

## Checklist
- [x] Add failing tests for status transition rules
- [x] Implement enum/entity transition changes
- [x] Implement sync NOT_RELEASED marking
- [x] Add V7 migration
- [x] Update OpenAPI
- [x] Run targeted tests
- [x] Run full test
- [ ] Commit by meaningful unit
- [ ] Push branch and open PR

## Test Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests 'com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecordTest' --tests 'com.chukchuk.haksa.application.portal.SyncAcademicRecordServiceTest.executeForRefreshPortalData_marksLectureEvaluationNotReleasedWhenGradeIsStillIp'` | RED: `NOT_RELEASED` enum/method missing, then PASS after implementation | 2026-06-21 |
| 2 | `./gradlew test --tests 'com.chukchuk.haksa.global.db.FlywayMigrationTest' --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest.staticOpenApiDocumentsNotReleasedLectureEvaluationStatus'` | RED: V7/OpenAPI enum missing, then PASS after migration/docs update | 2026-06-21 |
| 3 | `./gradlew test --tests 'com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecordTest' --tests 'com.chukchuk.haksa.application.portal.SyncAcademicRecordServiceTest' --tests 'com.chukchuk.haksa.domain.lectureevaluations.service.LectureEvaluationServiceUnitTests' --tests 'com.chukchuk.haksa.global.db.FlywayMigrationTest' --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'` | PASS | 2026-06-21 |
| 4 | `./gradlew test` | PASS | 2026-06-21 |
