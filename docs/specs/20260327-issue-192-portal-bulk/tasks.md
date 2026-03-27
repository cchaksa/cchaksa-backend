# Tasks

## Checklist
- [ ] Domain tests written (N/A)
- [ ] Application layer updated (SyncAcademicRecordService diff 최적화)
- [ ] Infrastructure layer updated (StudentCourseRepository batch delete)
- [ ] Global/config reviewed (hibernate jdbc batch 설정)
- [ ] API/controller updated (N/A)
- [x] Documentation updated (spec bundle)

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | PASS | 2026-03-27 |

## Notes
- Observation: batch 속성 적용 후 실제 성능 로그 비교 필요.
