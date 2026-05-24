# Issue 230 외국어 졸업 인증 표시 Spec

## 1. Feature Overview
- Purpose: 사용자 졸업 요건 조회 응답에서 외국어 졸업 인증 통과 여부를 보여준다.
- Scope
  - In: 크롤러 `flangPassGb` 매핑, 외국어 인증 저장, 졸업 요건 응답 확장, 캐시 무효화.
  - Out: 백그라운드 backfill, 별도 API 추가.
- Expected Impact: 기존 `/api/graduation/progress` 응답에 외국어 인증 필드가 추가된다.
- Stakeholder Confirmation: 2026-05-25 사용자 요청으로 구현 진행 승인. 커밋은 사용자가 직접 수행한다.

## 2. Domain Rules
- Rule 1: 크롤러 `flangPassGb`의 `Y`는 통과, `N`은 미통과로 변환한다.
- Rule 2: 기존 사용자처럼 저장값이 없으면 새로고침 필요 상태로 응답한다.
- Rule 3: 외국어 인증 동기화는 다른 졸업 진행 상태 필드를 변경하지 않는다.
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
