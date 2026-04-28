# 20260428 Issue 211 CI Reset Academic Data Fix

## 1. Feature Overview
- Purpose: `dev` 브랜치 CI에서 발생한 `Student.resetAcademicData()` 컴파일 실패를 수정한다.
- Scope
  - In: 포털 기존 학생 재사용 경로의 학사 데이터 초기화 호출을 현재 삭제 쿼리 기반 서비스 흐름과 정합화한다.
  - Out: 포털 API 계약, 학사 데이터 저장 구조, 신규 기능 추가는 제외한다.
- Expected Impact: `./gradlew check`의 `:compileJava` 실패가 해소되고 기존 포털 재연동 동작은 유지된다.
- Stakeholder Confirmation: 2026-04-28 사용자 요청 "수정하고 dev로 PR 올려", 이슈 번호 `211` 제공으로 구현 승인.

## 2. Domain Rules
- Rule 1: 기존 학생을 다른 사용자 포털 연결에 재사용할 때 이전 학사 기록은 제거되어야 한다.
- Rule 2: 학사 기록 삭제는 `StudentService.resetBy(UUID studentId)`의 명시 삭제 쿼리를 사용한다.
- Rule 3: 명시 삭제 후 현재 영속성 컨텍스트의 `Student` 학사 연관관계 컬렉션도 비운다.
- Mutable Rules: 포털 연결 중 기존 학생 재사용 시점의 초기화 호출 위치.
- Immutable Rules: 외부 API 계약, 엔티티 필드, 트랜잭션 경계, 학생-사용자 연결 정책.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 기존 학생 포털 연결 재사용
  - Trigger: 포털 초기화 중 기존 pending 학생 또는 현재 사용자 학생이 존재한다.
  - Actor: 포털 연결 처리 로직.
  - Steps: 기존 학생 조회, 학사 데이터 초기화, 학생 정보 갱신, 사용자 연결 갱신, 저장.
  - Expected Result: 컴파일 가능한 서비스 호출로 학사 데이터가 삭제되고 연결 갱신이 완료된다.

### Exception / Boundary Flow
- Scenario Name: 기존 학생이 없는 최초 연결
  - Condition: 재사용할 학생이 존재하지 않는다.
  - Expected Behavior: 기존 신규 학생 생성 경로가 그대로 수행된다.

## 4. Transaction / Consistency
- Transaction Start Point: `UserPortalConnectionRepository.initializePortalConnection`.
- Transaction End Point: 메서드 반환 시점.
- Atomicity Scope: 기존 학생 학사 데이터 삭제, 학생 정보 갱신, 사용자 연결 저장.
- Eventual Consistency Allowed: 허용하지 않는다.

## 5. API List (필요 시)
- Endpoint: 변경 없음.
  - Method: 변경 없음.
  - Request DTO: 변경 없음.
  - Response DTO: 변경 없음.
  - Authorization: 변경 없음.
  - Idempotency: 변경 없음.

## 6. Exception Policy
- Error Code: 변경 없음.
  - Condition: 변경 없음.
  - Message Convention: 변경 없음.
  - Handling Layer: 변경 없음.
  - User Exposure: 변경 없음.

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [ ] Phase 3 Application complete
- [ ] Phase 4 Infrastructure complete
- [ ] Phase 5 Global/Config complete
- [ ] Phase 6 API/Controller complete

## 8. Generated File List
- Path: `src/main/java/com/chukchuk/haksa/domain/student/model/Student.java`
  - Description: 현재 영속성 컨텍스트의 학사 연관관계 컬렉션을 비우는 메서드 추가.
  - Layer: Domain model.
- Path: `src/main/java/com/chukchuk/haksa/domain/user/repository/UserPortalConnectionRepository.java`
  - Description: 기존 학생 재사용 시 `StudentService.resetBy` 호출 후 엔티티 컬렉션 상태 초기화.
  - Layer: Domain service orchestration.
- Path: `src/test/java/com/chukchuk/haksa/domain/user/repository/UserPortalConnectionRepositoryTests.java`
  - Description: 기존 학생 재사용 시 `StudentService.resetBy`와 컬렉션 초기화 호출 검증.
  - Layer: Domain test.
- Path: `src/test/java/com/chukchuk/haksa/domain/student/model/StudentTests.java`
  - Description: `Student` 학사 연관관계 컬렉션 초기화 동작 검증.
  - Layer: Domain test.
