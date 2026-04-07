# Tasks Template

## Checklist
- [x] `FacultyDivision` 열거형에 `기타` 상수 추가
- [x] `CourseOfferingService.resolveFacultyDivision`에서 미정의 값을 `기타`로 매핑
- [x] `CourseOfferingServiceUnitTests`에 미정의 값 케이스 추가
- [x] `./gradlew test --tests "*CourseOfferingServiceUnitTests"`
- [x] `./gradlew test`

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "*CourseOfferingServiceUnitTests"` | Pass | 2026-04-07 |
| 2 | `./gradlew test` | Pass | 2026-04-07 |

## Notes
- Observation: 포털에서 신규 이수 구분이 들어올 때도 데이터 적재가 중단되지 않아야 한다.
