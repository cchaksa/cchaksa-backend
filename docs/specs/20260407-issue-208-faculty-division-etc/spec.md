# Spec Template

## 1. Feature Overview
- Purpose: 포털에서 전달하는 과목 이수 구분 문자열이 기존 열거형에 존재하지 않을 때도 안전하게 저장하도록 `기타` 구분을 도입한다.
- Scope
  - In:
    - `FacultyDivision` 열거형에 `기타` 값을 추가
    - 포털 연동 시 이수 구분 해석 로직(`CourseOfferingService.resolveFacultyDivision` 등)
    - 관련 단위 테스트 보강
    - `/api/graduation/progress` 응답에서 `기타` 영역을 캐시/요구사항 테이블에 의존하지 않고 동적으로 추가하며, 필요 학점은 0, 이수 학점은 실제 합계를 반환
    - `/api/academic/record` 응답 구조를 `기타` 과목 전용 리스트를 포함하도록 확장하고, 분류 로직 업데이트
  - Out:
    - 졸업 요건 계산 로직
    - 평가 방식 열거형/해석 방식
    - 외부 API 계약 변경
- Expected Impact: 미정의 이수 구분으로 인해 발생하던 예외가 제거되고, 모든 미지원 값이 `기타`로 집계된다.
- Stakeholder Confirmation: 2026-04-07 사용자 요청(이슈 #208)으로 OK to implement

## 2. Domain Rules
- Rule 1: 포털이 내려주는 정의된 이수 구분(`중핵`, `기교`, `선교`, `소교`, `전교`, `전취`, `전핵`, `전선`, `일선`, `복선`, `복핵`, `복교`)은 동일한 열거형 값으로 저장된다.
- Rule 2: null/blank 이수 구분은 현재와 같이 `null`(미지정)로 남긴다.
- Rule 3: 위 목록에 없는 비어있지 않은 문자열은 모두 `FacultyDivision.기타`로 저장하고, 원본 문자열은 별도로 저장하지 않는다.
- Rule 4: 졸업 요건 API에서 `기타` 영역은 요구사항 테이블 여부와 무관하게 노출되며, `requiredCredits=0`, `earnedCredits`는 실제 이수 학점 합계로 노출된다.
- Rule 5: 학업 기록 API는 `기타` 전용 섹션을 갖고, 분류 로직이 `기타`를 교양/전공과 분리한다.
- Mutable Rules: 포털 측에서 새로운 문자열을 도입하면 언제든 `기타`로 분류된다.
- Immutable Rules: 정의된 기존 이수 구분 값과 매핑은 변경하지 않는다.

## 3. Use-case Scenarios
### Normal Flow
- Scenario Name: 포털 배치가 CourseOffering 생성
  - Trigger: 포털 연동 작업이 `CreateOfferingCommand` 리스트를 생성한다.
  - Actor: 포털 동기화 배치
  - Steps:
    1. 배치가 포털 데이터를 조회해 `CreateOfferingCommand`를 채운다.
    2. `CourseOfferingService.getOrCreateAll`이 호출되어 `resolveFacultyDivision`로 문자열을 열거형으로 변환한다.
    3. 열거형 변환이 성공하면 수강 편성 정보가 저장된다.
  - Expected Result: 기존과 동일하게 정상 저장된다.

### Exception / Boundary Flow
- Scenario Name: 정의되지 않은 이수 구분 수신
  - Condition: 포털이 `ABC`와 같이 열거형에 존재하지 않는 값을 내려준다.
  - Expected Behavior: `resolveFacultyDivision`가 `FacultyDivision.기타`를 반환하고, 배치는 예외 없이 계속 진행된다.

## 4. Transaction / Consistency
- Transaction Start Point: `CourseOfferingService.getOrCreateAll`
- Transaction End Point: 동일 메서드 종료 시점 (JPA flush)
- Atomicity Scope: 단일 `getOrCreateAll` 호출 내에서 생성되는 `CourseOffering`
- Eventual Consistency Allowed: 허용 (포털 재실행으로 보완 가능)

## 5. API List (필요 시)
- Endpoint: 해당 없음 (내부 배치 전용)

## 6. Exception Policy
- Error Code: 해당 없음 (Runtime 예외 제거)
  - Condition: 열거형 미존재
  - Message Convention: N/A (예외 자체가 발생하지 않음)
  - Handling Layer: Application 서비스에서 `기타`로 대체
  - User Exposure: 없음

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [x] Phase 2 Domain complete
- [x] Phase 3 Application complete
- [x] Phase 4 Infrastructure complete
- [ ] Phase 5 Global/Config complete
- [x] Phase 6 API/Controller complete

## 8. Generated File List
- Path: src/main/java/com/chukchuk/haksa/domain/course/model/FacultyDivision.java
  - Description: `기타` 열거형 추가
  - Layer: Domain
- Path: src/main/java/com/chukchuk/haksa/domain/course/service/CourseOfferingService.java
  - Description: 이수 구분 해석 시 미정의 값을 `기타`로 대체
  - Layer: Application
- Path: src/test/java/com/chukchuk/haksa/domain/course/service/CourseOfferingServiceUnitTests.java
  - Description: 미지원 값이 `기타`로 저장되는 단위 테스트 추가/보강
  - Layer: Application Tests
