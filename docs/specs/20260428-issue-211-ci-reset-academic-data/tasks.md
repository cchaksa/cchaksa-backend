# 20260428 Issue 211 CI Reset Academic Data Fix Tasks

## Checklist
- [x] Domain test expectation updated
- [x] Domain reset call updated
- [x] Student association clearing added
- [x] Infrastructure layer reviewed
- [x] Global/config reviewed
- [x] API/controller reviewed
- [x] Documentation updated

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepositoryTests --stacktrace --no-daemon` | Passed | 2026-04-28 |
| 2 | `./gradlew check --stacktrace --no-daemon` | Passed | 2026-04-28 |
| 3 | `./gradlew test --tests com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepositoryTests --tests com.chukchuk.haksa.domain.student.model.StudentTests --stacktrace --no-daemon` | Passed | 2026-04-28 |
| 4 | `./gradlew check --stacktrace --no-daemon` | Passed | 2026-04-28 |

## Notes
- Observation: CI failure reproduced at `:compileJava` because `UserPortalConnectionRepository` calls removed `Student.resetAcademicData()`.
- Verification: 동일 CI 명령 `./gradlew check --stacktrace --no-daemon` 성공.
- Review: DB 명시 삭제 후 현재 영속성 컨텍스트의 `Student` 컬렉션 상태를 함께 비우도록 반영.
