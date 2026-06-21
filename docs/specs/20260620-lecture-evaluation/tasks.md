# Issue 245 Tasks

## Checklist
- [x] Migration added
- [x] Domain model added
- [x] Repositories added
- [x] Service tests written
- [x] API/controller added
- [x] Sync detection added
- [x] OpenAPI updated
- [x] Targeted tests passed
- [x] Full test passed

## Implementation Tasks
- [x] Add `V5__create_lecture_evaluation_tables.sql`
- [x] Add `V6__replace_lecture_evaluation_flags_with_status.sql`
- [x] Add lecture evaluation domain models and enum
- [x] Add DTOs under `domain/lectureevaluations/dto`
- [x] Add repositories under `domain/lectureevaluations/repository`
- [x] Add `LectureEvaluationService`
- [x] Add `LectureEvaluationController` and docs
- [x] Add `LectureEvaluationProperties` and application config
- [x] Extend `SemesterAcademicRecord` with evaluation status
- [x] Reuse existing `StudentCourseRepository.findByStudentIdAndYearAndSemester` for evaluation target query
- [x] Add sync detection in `SyncAcademicRecordService`
- [x] Update `ErrorCode`
- [x] Update `openapi.yaml`
- [x] Add focused tests

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests 'com.chukchuk.haksa.domain.lectureevaluations.service.LectureEvaluationServiceUnitTests'` | RED: missing production classes, then PASS after implementation | 2026-06-20 |
| 2 | `./gradlew test --tests 'com.chukchuk.haksa.application.portal.SyncAcademicRecordServiceTest.executeForRefreshPortalData_marksLectureEvaluationRequiredWhenGradeChangesFromIpToCompleted'` | RED: required marker not invoked, then PASS after sync detection | 2026-06-20 |
| 3 | `./gradlew test --tests 'com.chukchuk.haksa.global.db.FlywayMigrationTest' --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'` | PASS | 2026-06-20 |
| 4 | `./gradlew test --tests 'com.chukchuk.haksa.domain.lectureevaluations.service.LectureEvaluationServiceUnitTests' --tests 'com.chukchuk.haksa.application.portal.SyncAcademicRecordServiceTest'` | PASS | 2026-06-20 |
| 5 | `./gradlew test` | PASS | 2026-06-20 |
| 6 | `./gradlew test --tests 'com.chukchuk.haksa.domain.lectureevaluations.service.LectureEvaluationServiceUnitTests' --tests 'com.chukchuk.haksa.application.portal.SyncAcademicRecordServiceTest.executeForRefreshPortalData_marksLectureEvaluationPendingWhenGradeChangesFromIpToCompleted'` | RED: missing status/skip API, then PASS after implementation | 2026-06-21 |
| 7 | `./gradlew test --tests 'com.chukchuk.haksa.global.db.FlywayMigrationTest' --tests 'com.chukchuk.haksa.global.config.OpenApiResponseContractTest'` | RED: V6 migration missing, then PASS after migration/OpenAPI update | 2026-06-21 |
| 8 | `./gradlew test` | PASS | 2026-06-21 |

## Notes
- Observation:
  - Branch is `feat/245`, tracking `origin/feat/245`.
- Observation:
  - Existing untracked files were present before this work and should not be modified unless directly required.
- Observation:
  - `POST /api/lecture-evaluations` success response follows existing `MessageOnlyResponse` style.
- Decision:
  - `evaluationStatus=null` means no evaluation flow exists yet; FE should show the grade card only for `PENDING`.
