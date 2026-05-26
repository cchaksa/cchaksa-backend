# Issue 230 Context Notes

- 2026-05-25: 사용자 확인으로 `flangPassGb`는 `Y/N` 계약으로 고정했다.
- 2026-05-25: 기존 사용자는 값이 없을 수 있으므로 `null`과 `languageCertNeedsRefresh`로 미확인 상태를 표시한다.
- 2026-05-25: 백그라운드 backfill은 범위에서 제외하고 사용자 포털 새로고침으로 갱신한다.
- 2026-05-25: raw payload는 기존 `student`와 사용자 언급의 `studentInfo`를 모두 받을 수 있도록 `@JsonAlias("studentInfo")`를 사용했다.
- 2026-05-25: 테스트 실행은 Java 17로 Gradle을 실행해야 통과한다. 기본 Java 24에서는 Gradle test task 구성 오류가 발생한다.
- 2026-05-25: 정적 Swagger 문서 `src/main/resources/public/openapi.yaml`에 졸업 요건 조회 endpoint와 외국어 인증 응답 필드를 추가했다.
- 2026-05-25: `checked_at`은 이번 기능 요구사항에 필요 없고 의미가 애매해 외국어 인증 동기화에서 변경하지 않도록 수정했다.
- 2026-05-26: 사용자와 논의해 외국어 인증 통과 여부는 기존 졸업 진행 응답에 유지하고, 학과별 기준은 별도 정책 모델로 분리하기로 했다.
- 2026-05-26: `Department`는 포털 학과 코드 저장과 자동 생성 책임만 유지하고, 외국어 인증 기준은 `departmentCode + admissionYear`로 정책 그룹에 명시 매핑한다.
- 2026-05-26: 이번 작업 범위는 엔티티와 enum 추가까지이며, 기준 조회 API와 seed 데이터 적재는 후속 작업으로 분리한다.
- 2026-05-26: `LanguageCertPolicyGroup`, `LanguageCertRequirement`, `DepartmentLanguageCertPolicyMapping`과 `LanguageCertTestType`, `LanguageCertMatchStatus`를 추가했다.
- 2026-05-26: `LanguageCertPolicyModelTests`를 RED/GREEN 순서로 실행해 정책 모델 생성과 입학년도 구간 매핑을 검증했다.
- 2026-05-27: 사용자 승인으로 후속 범위를 seed SQL과 기준 조회 API까지 확장했다.
- 2026-05-27: 정책 데이터는 자동 추론 로직이 아니라 명시 seed SQL로 관리한다.
- 2026-05-27: CSV 384개 학과 코드는 `VERIFIED` 331개, `INFERRED` 15개, `UNMAPPED` 38개로 seed에 반영했다.
- 2026-05-27: 기준 조회는 `student.major`가 있으면 major 학과 코드, 없으면 department 학과 코드를 사용한다.
- 2026-05-27: `INFERRED`는 기준 목록을 반환하고, `UNMAPPED` 또는 매핑 없음은 200 응답과 빈 기준 목록을 반환한다.
- 2026-05-27: 사용자 요청으로 DDL/seed SQL 파일은 저장소에서 제거하고 채팅으로만 전달한다.
