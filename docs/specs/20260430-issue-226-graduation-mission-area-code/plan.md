# Plan

## Architecture / Layering
- Domain impact: 없음. `CourseOffering`, `LiberalArtsAreaCode`, `FacultyDivision` enum 모델/규칙 변경 없음.
- Application orchestration: 없음. graduation 모듈은 controller→repository 직결 구조이며 `GraduationService`는 단순 위임이라 변경 불요.
- Infrastructure touchpoints:
  - `GraduationQueryRepository.getLatestValidCourses` native SQL — 9번째 컬럼으로 `co.area_code` 추가 SELECT. 기존 `DISTINCT ON` / `ORDER BY` 절은 그대로 유지(영역 식별 컬럼이 아니므로 영향 없음).
  - row→`CourseInternalDto` 매핑 — 신규 컬럼을 10번째 인자로 추가.
  - `CourseInternalDto` — `liberalAreaCode: Integer (nullable)` 필드 추가. `@AllArgsConstructor` 시그니처 확장.
  - `GraduationQueryRepository.toCourseResponseDto` — 새 인자 패스스루로 `CourseDto.liberalAreaCode` 채움.
  - `getStudentAreaProgress` / `getDualMajorAreaProgress` 본문은 변경 불요(매퍼 통해 자동 반영).
  - 매퍼 공유: `toCourseResponseDto`가 두 흐름에서 동일하게 호출되며, 본 스펙은 양쪽 모두에 적용한다(clarify Decision 4).
- Global/config changes: 글로벌 Jackson에 `NON_NULL`이 이미 적용돼 있는지 점검(`application.yml: spring.jackson.default-property-inclusion: non_null` 또는 `ObjectMapper` 빈). 미적용이면 `CourseDto`에 `@JsonInclude(JsonInclude.Include.NON_NULL)`을 클래스 레벨로 적용.

## Data / Transactions
- Repositories touched: `GraduationQueryRepository` (native query 1군데 + 매퍼 1개). JPA 엔티티/매핑 변경 없음.
- Transaction scope: 기존과 동일. `GraduationController.getGraduationProgress`가 단일 read-only 호출.
- Consistency expectations: 변경 없음. 응답 한 시점의 일관성만 요구.

## Testing Strategy
- Domain tests: N/A (도메인 변경 없음).
- Application tests: graduation 모듈에 application layer가 없어 N/A.
- Integration/API tests:
  - `CourseDto` 직렬화 단위 테스트 — `liberalAreaCode` 정수 → 키 포함, null → 키 omit 검증.
  - `GraduationQueryRepository` 통합/단위 테스트 — 다음 케이스 검증.
    - 선교 영역 row의 `area_code`가 정수 → `CourseDto.liberalAreaCode` 동일 정수.
    - 선교 영역 row의 `area_code`가 NULL → `CourseDto.liberalAreaCode = null`.
    - 비-선교 영역 row → `CourseDto.liberalAreaCode = null`.
    - 한 학생이 선교 N건 + 비-선교 M건 → 각 row의 area_code가 독립적으로 패스스루됨.
    - 단일전공 시나리오(`getStudentAreaProgress`)와 복수전공 시나리오(`getDualMajorAreaProgress`) 모두에서 매퍼가 동일하게 동작함을 검증.
  - 컨트롤러 e2e 테스트는 기존이 있으면 응답 스키마 변경 검증을 추가, 없으면 본 스펙에서는 신설하지 않음.
- Additional commands: `./gradlew test`. 글로벌 Jackson 설정 변경 시 `./gradlew build`도 수행.

## Rollout Considerations
- Backward compatibility: 추가 nullable 필드만 노출하고 기존 키/타입은 변경 없음. NON_NULL omit 정책상 비-선교 응답 shape는 사실상 유지됨. Backward-compatible.
- Observability / metrics: 추가 없음. 기존 `[BIZ] graduation.progress.query.done` 로그 유지.
- Feature flags / toggles: 미사용. 단순 추가 nullable 필드라 토글 불요.
- Data risk: 이전 분석상 `course_offerings.area_code`에 historical NULL 데이터가 잔존할 수 있으나 본 스펙은 표시 전용이라 영향이 격리됨. backfill은 별도 이슈로 분리(clarify.md Follow-ups 참조).
- Cross-flow note: `toCourseResponseDto` 공유로 인해 단일전공·복수전공 응답에 동시 반영됨 (Decision 4). dual-major 졸업 요건에 선교 영역이 등장하는 학과라면 응답에 동일하게 노출된다.
