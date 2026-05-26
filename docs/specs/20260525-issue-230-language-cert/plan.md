# Issue 230 Plan

## Architecture / Layering
- Domain impact: `StudentGraduationProgress`가 외국어 인증 생성/갱신 규칙을 가진다. 외국어 인증 기준 조회를 위한 정책 그룹, 시험 기준, 학과 코드 매핑 엔티티와 enum을 추가한다.
- Application orchestration: `PortalSyncService`가 포털 동기화 성공 후 외국어 인증 저장을 호출한다. `LanguageCertRequirementService`는 로그인 학생의 학과 코드와 입학년도로 기준 그룹을 조회한다.
- Infrastructure touchpoints: raw portal DTO와 mapper가 `flangPassGb`를 내부 boolean으로 변환한다. 운영 DB 반영용 DDL과 seed SQL은 `docs/sql`에 둔다.
- Global/config changes: 없음.

## Data / Transactions
- Repositories touched: `StudentGraduationProgressRepository`, `DepartmentLanguageCertPolicyMappingRepository`, `LanguageCertRequirementRepository` 추가.
- Transaction scope: 기존 포털 동기화 트랜잭션에 참여한다.
- Consistency expectations: 저장 후 학생 학업 캐시를 삭제해 다음 조회가 최신 값을 읽는다. 외국어 인증 동기화는 `checked_at`을 변경하지 않는다.
- New policy tables: `language_cert_policy_groups`, `language_cert_requirements`, `department_language_cert_policy_mappings`.
- Policy lookup key: `departmentCode`와 `admissionYear`로 기준 그룹을 찾는다.
- Policy seed source: `departments_rows.csv` 전체 384개 학과 코드와 사진 기준표를 명시 매핑한다.

## Testing Strategy
- Domain tests: 외국어 인증 row 생성/갱신.
- Domain model tests: 외국어 인증 정책 엔티티 factory와 학과 코드/입학년도 적용 여부.
- Application tests: 초기 연동/새로고침 저장과 캐시 무효화 호출.
- Service tests: major 우선, department fallback, `INFERRED`, `UNMAPPED`, 매핑 없음 케이스 확인.
- Repository tests: 학과 코드와 입학년도 구간으로 적용 가능한 매핑 조회 확인.
- Integration/API tests: 졸업 요건 응답 필드와 외국어 인증 기준 조회 응답 확인.
- Additional commands: `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*LanguageCert*"`, `JAVA_HOME=/Users/keemhoeyune/Library/Java/JavaVirtualMachines/temurin-17.0.18/Contents/Home ./gradlew test --tests "*GraduationControllerApiIntegrationTest" --tests "*LanguageCert*"`, `git diff --check`

## Rollout Considerations
- Backward compatibility: 기존 사용자 null 상태를 새로고침 필요로 노출한다.
- Observability / metrics: 기존 포털 후처리 로그를 사용한다.
- Feature flags / toggles: 없음.
