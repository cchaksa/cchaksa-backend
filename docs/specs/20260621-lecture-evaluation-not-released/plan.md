# Plan

## Implementation Plan
- Add `NOT_RELEASED` to `LectureEvaluationStatus`.
- Extend `SemesterAcademicRecord` with:
  - `markLectureEvaluationNotReleased()`
  - `markLectureEvaluationPending()` that allows `null` and `NOT_RELEASED` to become `PENDING`, but preserves `SKIPPED`/`COMPLETED`.
- Update `SyncAcademicRecordService`:
  - collect semesters that have any changed or existing target courses
  - mark semester `NOT_RELEASED` when all semester courses are still `IP`
  - mark semester `PENDING` when grade publication is detected
- Add Flyway `V7__add_not_released_lecture_evaluation_status.sql`:
  - drop/recreate check constraint with `NOT_RELEASED`
  - initialize null rows using joined `student_courses`/`course_offerings`
- Update OpenAPI enum and descriptions.
- Add tests before implementation:
  - entity state transitions
  - sync marks NOT_RELEASED for all-IP semester
  - sync transitions NOT_RELEASED to PENDING on grade publication
  - migration includes V7 and check constraint-compatible status
  - OpenAPI includes `NOT_RELEASED`

## Risk Review
- Migration can mark many historical semesters as `PENDING`; this is acceptable because main API is target-semester scoped.
- `NULL` remains valid for rows with no matching course data.
- `NOT_RELEASED` should not allow submit/skip; current service already allows only `PENDING`.
