# Tasks Template

## Checklist
- [x] `FacultyDivision` 열거형에 `기타` 상수 추가
- [x] `CourseOfferingService.resolveFacultyDivision`에서 미정의 값을 `기타`로 매핑
- [x] `CourseOfferingServiceUnitTests`에 미정의 값 케이스 추가
- [x] `/api/graduation/progress`에 `기타` 영역 동적 추가 및 매직 넘버 처리
- [x] `/api/academic/record` 응답 구조 수정 및 분류 로직 업데이트
- [x] Graduation Query Repository 단위 테스트 추가
- [x] Academic Record Service 단위 테스트/컨트롤러 테스트 업데이트
- [x] `course_offerings.raw_faculty_division_name` 추가 SQL 작성
- [x] 미지원 이수 구분 raw 보존 테스트 추가
- [x] 미지원 이수 구분 재연동 키 재사용 테스트 추가
- [x] `CourseOffering` raw 컬럼 및 생성자 반영
- [x] `CourseOfferingService` canonical/raw 분리 및 key 정규화 반영
- [x] `./gradlew test --tests "*CourseOfferingServiceUnitTests"`
- [x] `./gradlew test --tests "*AcademicRecordServiceUnitTests"`
- [x] `./gradlew test --tests "*GraduationQueryRepository*"`
- [x] `./gradlew test`
- [x] `/api/academic/record` 과목 응답 `rawAreaType` 테스트 추가
- [x] `StudentCourseDto.CourseDetailDto`에 nullable `rawAreaType` 추가
- [x] `StudentCourseDto.from`에서 `CourseOffering.rawFacultyDivisionName` 매핑
- [x] Academic Record 관련 테스트 갱신

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "*CourseOfferingServiceUnitTests"` | Pass | 2026-04-07 |
| 2 | `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Pass | 2026-04-07 |
| 3 | `./gradlew test --tests "*GraduationQueryRepository*"` | Pass (rerun after review) | 2026-04-07 |
| 4 | `./gradlew test` | Pass (rerun after review) | 2026-04-07 |
| 5 | `./gradlew test --tests "*CourseOfferingServiceUnitTests"` | Fail as RED: `getRawFacultyDivisionName()` missing | 2026-05-26 |
| 6 | `./gradlew test --tests "*CourseOfferingServiceUnitTests"` | Pass | 2026-05-26 |
| 7 | `./gradlew test --tests "*SyncAcademicRecordServiceTest"` | Pass | 2026-05-26 |
| 8 | `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Pass | 2026-05-26 |
| 9 | `./gradlew test --tests "*GraduationQueryRepository*"` | Pass after serial rerun; first parallel run failed due shared Gradle test output file | 2026-05-26 |
| 10 | `./gradlew test` | Pass | 2026-05-26 |
| 11 | `./gradlew build` | Pass | 2026-05-26 |
| 12 | `./gradlew test --tests "*StudentCourseDtoTests"` | Fail as RED: `rawAreaType()` missing | 2026-05-26 |
| 13 | `./gradlew test --tests "*StudentCourseDtoTests"` | Pass | 2026-05-26 |
| 14 | `./gradlew test --tests "*AcademicRecordServiceUnitTests"` | Pass | 2026-05-26 |
| 15 | `./gradlew test --tests "*AcademicRecordControllerApiIntegrationTest"` | Pass | 2026-05-26 |
| 16 | `./gradlew test` | Pass | 2026-05-26 |
| 17 | `./gradlew build` | Pass | 2026-05-26 |
| 18 | `git diff --check` | Pass | 2026-05-26 |
| 19 | `rg -n "rawAreaType|rawFacultyDivisionName" src/main/java/com/chukchuk/haksa/domain/graduation src/main/java/com/chukchuk/haksa/application src/main/java/com/chukchuk/haksa/infrastructure` | No matches; graduation response path unchanged | 2026-05-26 |

## Notes
- Observation: 포털에서 신규 이수 구분이 들어올 때도 데이터 적재가 중단되지 않아야 한다.
- Observation: raw 원본은 API 계약이 아니라 향후 마이그레이션과 재연동 안정성을 위한 내부 데이터다.
