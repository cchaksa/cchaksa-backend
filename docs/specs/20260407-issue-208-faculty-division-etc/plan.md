# Plan Template

## Architecture / Layering
- Domain impact: `FacultyDivision` 열거형에 `기타` 상수를 추가하고, enum 순서를 유지한 채 말미에 배치한다.
- Application orchestration: `CourseOfferingService.resolveFacultyDivision`에서 `IllegalArgumentException` 발생을 try/catch로 흡수하고 `FacultyDivision.기타`로 반환한다.
- Graduation API: `GraduationQueryRepository`가 요구사항 테이블 조회 결과와 별개로 `기타` 영역을 동적으로 추가하고 고정 매직 넘버(0, 0)를 설정한다.
- Academic Record API: `AcademicRecordResponse.Courses` 레이아웃을 확장하고 `AcademicRecordService` 분류 로직을 `기타` 섹션을 포함하도록 업데이트한다.
- Infrastructure touchpoints: 없음 (native query 결과 후처리만 변경).
- Global/config changes: 없음.

## Data / Transactions
- Repositories touched: `CourseOfferingRepository` (간접적으로 동일 메서드를 통해 동작하므로 변경 없음).
- Transaction scope: `CourseOfferingService.getOrCreateAll` 기존 트랜잭션 재사용.
- Consistency expectations: 새 enum 값은 기존 행과 동일하게 저장되며, 재시도 시 동일 offering key가 유지된다.

## Testing Strategy
- Domain tests: 단위 테스트만으로 충분 (별도 Domain test 없음).
- Application tests: `CourseOfferingServiceUnitTests`에 "알 수 없는 이수 구분" 케이스를 추가해 `FacultyDivision.기타`로 저장되는지 검증.
- Graduation repository tests: 요구사항 테이블에 없는 `기타` 수강 이력이 있을 때 `AreaProgressDto`가 매직 넘버와 함께 추가되는지 단위 테스트로 보장.
- Academic record tests: `AcademicRecordServiceUnitTests`에서 `기타` 과목이 전용 섹션으로 분리되는지 검증하고, WebMvc 테스트 mock 응답 스키마 업데이트.
- Additional commands: `./gradlew test --tests "*CourseOfferingServiceUnitTests"`, `./gradlew test --tests "*AcademicRecordServiceUnitTests"`, `./gradlew test --tests "*GraduationQueryRepository*"` (신규), 최종 `./gradlew test`.

## Rollout Considerations
- Backward compatibility: 기존 enum 값 유지, DB 스키마 영향 없음.
- Observability / metrics: 필요 없음.
- Feature flags / toggles: 불필요.
