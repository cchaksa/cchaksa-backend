# Issue 230 외국어 졸업 인증 표시 Spec

## 1. Feature Overview
- Purpose: 사용자 졸업 요건 조회 응답에서 외국어 졸업 인증 통과 여부를 보여준다.
- Scope
  - In: 크롤러 `flangPassGb` 매핑, 외국어 인증 저장, 졸업 요건 응답 확장, 캐시 무효화, 사용자 학과별 외국어 인증 기준 정책 엔티티와 enum, 기준 조회 API, 운영 DB 반영용 DDL과 seed SQL.
  - Out: 백그라운드 backfill, 관리자 기준 수정 화면/API.
- Expected Impact: 기존 `/api/graduation/progress` 응답에 외국어 인증 필드가 추가되고, `/api/graduation/language-cert/requirement`로 사용자 학과별 기준을 조회할 수 있다.
- Stakeholder Confirmation: 2026-05-25 사용자 요청으로 구현 진행 승인. 2026-05-27 사용자 승인으로 seed SQL과 기준 조회 API까지 범위를 확장했다. 커밋은 사용자가 직접 수행한다.

## 2. Domain Rules
- Rule 1: 크롤러 `flangPassGb`의 `Y`는 통과, `N`은 미통과로 변환한다.
- Rule 2: 기존 사용자처럼 저장값이 없으면 새로고침 필요 상태로 응답한다.
- Rule 3: 외국어 인증 동기화는 다른 졸업 진행 상태 필드와 `checked_at`을 변경하지 않는다.
- Rule 4: 외국어 인증 통과 여부는 포털 동기화 결과로 저장하고, 외국어 인증 기준은 학과 코드와 입학년도 구간으로 정책 그룹에 매핑한다.
- Rule 5: 외국어 인증 기준 정책은 `Department` 자동 생성과 분리해 명시 매핑으로 관리한다.
- Rule 6: 기준 조회 시 학생의 `major`가 있으면 major 학과 코드를 우선 사용하고, 없으면 department 학과 코드를 사용한다.
- Rule 7: `INFERRED` 매핑은 기준 목록을 반환하고, `UNMAPPED` 또는 매핑 없음은 200 응답과 빈 기준 목록을 반환한다.
- Mutable Rules: 응답 필드명, backfill 방식.
- Immutable Rules: 외국어 인증은 학점 영역이 아니므로 `AreaProgressDto`에 포함하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 신규 포털 동기화 후 졸업 요건 조회
  - Trigger: 포털 연동 또는 새로고침 성공.
  - Actor: 로그인 사용자.
  - Steps: 크롤러 값 저장 후 졸업 요건 조회.
  - Expected Result: `languageCertFulfilled`가 `true` 또는 `false`, `languageCertNeedsRefresh`가 `false`로 반환된다.

### Exception / Boundary Flow
- Scenario Name: 기존 사용자 미동기화 상태
  - Condition: `student_graduation_progress` row가 없거나 `language_cert_fulfilled`가 `null`.
  - Expected Behavior: `languageCertFulfilled`는 `null`, `languageCertNeedsRefresh`는 `true`로 반환된다.

## 4. Transaction / Consistency
- Transaction Start Point: 포털 연동 또는 새로고침 후처리 트랜잭션.
- Transaction End Point: 사용자 학업 정보와 외국어 인증 저장 완료.
- Atomicity Scope: 동일 포털 동기화 트랜잭션 내 저장.
- Eventual Consistency Allowed: 캐시 삭제 후 다음 졸업 요건 조회에서 최신 응답 생성.

## 5. API List
- Endpoint:
  - Method: `GET /api/graduation/progress`
  - Request DTO: 없음.
  - Response DTO: `GraduationProgressResponse`
  - Authorization: Bearer auth.
  - Idempotency: 조회 API이므로 멱등.
- Endpoint:
  - Method: `GET /api/graduation/language-cert/requirement`
  - Request DTO: 없음.
  - Response DTO: `LanguageCertRequirementResponse`
  - Authorization: Bearer auth.
  - Idempotency: 조회 API이므로 멱등.

## 6. Exception Policy
- Error Code:
  - Condition: 알 수 없는 `flangPassGb` 값.
  - Message Convention: 기존 포털 후처리 실패 정책을 따른다.
  - Handling Layer: 포털 매핑 또는 후처리.
  - User Exposure: 기존 스크래핑 실패 응답 경로를 따른다.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [x] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/repository/StudentGraduationProgressRepository.java`
  - Description: 학생별 졸업 진행 row 조회 repository.
  - Layer: Domain repository
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/service/StudentGraduationProgressService.java`
  - Description: 외국어 인증 저장 및 조회 서비스.
  - Layer: Domain service
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/model/LanguageCertPolicyGroup.java`
  - Description: 외국어 인증 기준 그룹 엔티티.
  - Layer: Domain model
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/model/LanguageCertRequirement.java`
  - Description: 기준 그룹별 시험 기준 엔티티.
  - Layer: Domain model
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/model/DepartmentLanguageCertPolicyMapping.java`
  - Description: 학과 코드와 입학년도 구간을 기준 그룹에 연결하는 엔티티.
  - Layer: Domain model
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/model/LanguageCertTestType.java`
  - Description: 외국어 인증 시험 종류 enum.
  - Layer: Domain model
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/model/LanguageCertMatchStatus.java`
  - Description: 학과-정책 매핑 신뢰 상태 enum.
  - Layer: Domain model
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/repository/DepartmentLanguageCertPolicyMappingRepository.java`
  - Description: 학과 코드와 입학년도 기준 정책 매핑 조회 repository.
  - Layer: Domain repository
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/repository/LanguageCertRequirementRepository.java`
  - Description: 정책 그룹별 시험 기준 조회 repository.
  - Layer: Domain repository
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/service/LanguageCertRequirementService.java`
  - Description: 로그인 학생에게 적용되는 외국어 인증 기준 조회 서비스.
  - Layer: Domain service
- Path: `src/main/java/com/chukchuk/haksa/domain/graduation/dto/LanguageCertRequirementResponse.java`
  - Description: 외국어 인증 기준 조회 응답 DTO.
  - Layer: Domain DTO
- Path: `docs/sql/20260527-language-cert-policy-ddl.sql`
  - Description: 외국어 인증 기준 운영 테이블 생성 SQL.
  - Layer: SQL document
- Path: `docs/sql/20260527-language-cert-policy-seed.sql`
  - Description: 정책 그룹, 시험 기준, CSV 학과 코드 매핑 seed SQL.
  - Layer: SQL document
