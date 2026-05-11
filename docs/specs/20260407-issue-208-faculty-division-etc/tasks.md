# Tasks Template

## Checklist
- [x] `FacultyDivision` 열거형에 `기타` 상수 추가
- [x] `CourseOfferingService.resolveFacultyDivision`에서 미정의 값을 `기타`로 매핑
- [x] `CourseOfferingServiceUnitTests`에 미정의 값 케이스 추가
- [x] `/api/graduation/progress`에 `기타` 영역 동적 추가 및 매직 넘버 처리
- [x] `/api/academic/record` 응답 구조 수정 및 분류 로직 업데이트
- [x] Graduation Query Repository 단위 테스트 추가
- [x] Academic Record Service 단위 테스트/컨트롤러 테스트 업데이트
- [x] `./gradlew test --tests "*CourseOfferingServiceUnitTests"`
- [x] `./gradlew test --tests "*AcademicRecordServiceUnitTests"`
- [x] `./gradlew test --tests "*GraduationQueryRepository*"`
- [x] `./gradlew test`

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "*CourseOfferingServiceUnitTests"` | Pass | 2026-04-07 |
| 2 | `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Pass | 2026-04-07 |
| 3 | `./gradlew test --tests "*GraduationQueryRepository*"` | Pass (rerun after review) | 2026-04-07 |
| 4 | `./gradlew test` | Pass (rerun after review) | 2026-04-07 |

## Notes
- Observation: 포털에서 신규 이수 구분이 들어올 때도 데이터 적재가 중단되지 않아야 한다.
