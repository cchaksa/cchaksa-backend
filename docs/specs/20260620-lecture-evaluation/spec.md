# Issue 245: 성적 공개 시 강의평가 카드 노출 및 평가 데이터 수집

## 1. Feature Overview
- Purpose:
  - 현재 수료 학기 성적이 공개된 사용자가 성적 카드 화면에서 해당 학기 강의평가를 제출할 수 있게 한다.
- Scope
  - In:
    - `semester_academic_records` 강의평가 상태 컬럼 추가
    - `lectureevaluations` 신규 패키지 추가
    - 강의평가 상태와 성적 카드 조회 API 추가
    - 학기 단위 강의평가 일괄 제출 API 추가
    - 강의평가 건너뛰기 API 추가
    - 강의평가 본문 테이블과 선택 태그 테이블 추가
    - 포털 동기화 중 `IP -> completed grade` 감지
    - OpenAPI 문서와 Flyway migration 추가
  - Out:
    - 과거 학기 강의평가 별도 진입점
    - 저장된 강의평가 조회/노출 API
    - 평가 태그 관리자 API
    - 평가 통계, 랭킹, 추천 기능
    - FE 화면 구현
- Expected Impact:
  - 성적 공개 시점에 현재 수료 학기의 평가 대상 과목을 FE가 조회할 수 있다.
  - 사용자는 해당 학기 평가 대상 과목 전체를 한 번에 제출한다.
  - 저장 데이터는 추후 `course_id + professor_id` 기준 누적 조회에 사용할 수 있다.

## 2. Domain Rules
- Rule 1:
  - 현재 기능은 Spring 설정값 `lecture-evaluation.target-year`, `lecture-evaluation.target-semester`에 해당하는 메인 노출 대상 학기 1개만 대상으로 한다.
- Rule 2:
  - 현재 수료 학기 과목 중 하나라도 `student_courses.grade`가 `IP`에서 완료 성적으로 바뀌면 `lecture_evaluation_status = PENDING`으로 표시한다.
- Rule 3:
  - `lecture_evaluation_status`가 이미 `SKIPPED` 또는 `COMPLETED`인 학기는 다시 `PENDING`으로 되돌리지 않는다.
- Rule 4:
  - 강의평가 조회와 제출 대상은 해당 학기 수강 과목 중 `grade != IP`인 과목이다.
- Rule 5:
  - 제출은 학기 단위 일괄 제출만 허용하며, 평가 대상 과목 전체가 요청에 포함되어야 한다.
- Rule 6:
  - 중복 제출 방지는 `(student_id, year, semester, course_id, professor_id)` 기준으로 한다.
- Rule 7:
  - 성적 카드 응답 점수 필드는 `score` 하나만 사용하고, 값은 `student_courses.original_score`에서 가져온다. 원천 값이 null이면 응답도 null이다.
- Rule 8:
  - `target-semester`는 화면 표시용 1/2가 아니라 DB에 저장되는 semester code와 동일한 값이다. 현재 포털 payload 예시 기준 정규 1학기는 `10`, 정규 2학기는 `20`으로 저장될 수 있다.
- Rule 9:
  - `lecture_evaluation_status = null`은 평가 플로우가 아직 생성되지 않은 상태이다. FE는 `PENDING`만 성적 카드 노출 조건으로 사용한다.
- Mutable Rules:
  - API 성공 메시지 문구
  - enum key의 최종 영문 이름
  - 신규 ErrorCode 이름과 코드 번호
- Immutable Rules:
  - `offering_id`는 강의평가 저장 테이블에 저장하지 않는다.
  - 강의평가 본문과 선택 태그는 별도 테이블로 분리한다.
  - DB DDL은 Flyway migration으로만 반영한다.
  - 메인 노출 대상 학기는 DB 상태 컬럼으로 저장하지 않고 Spring configuration property로 임시 운영한다.

## 3. Use-case Scenarios
### Normal Flow: 성적 공개 후 강의평가 진입
- Trigger:
  - `/internal/scrape-results` 콜백 후 학업 데이터가 재동기화된다.
- Actor:
  - Portal callback / SyncAcademicRecordService / LectureEvaluationService / FE
- Steps:
  1. 백엔드가 기존 수강 기록과 포털 스냅샷을 비교한다.
  2. 수강 과목 중 `IP -> completed grade` 변경을 감지한다.
  3. 해당 `semester_academic_records` row의 `lecture_evaluation_status`를 `PENDING`으로 바꾼다.
  4. FE가 `GET /api/lecture-evaluations/required`를 호출한다.
  5. 백엔드가 설정값의 target year/semester 상태와 평가 대상 성적 카드 목록을 반환한다.
  6. FE가 해당 과목 전체에 대한 평가를 받아 `POST /api/lecture-evaluations`로 제출한다.
  7. 백엔드가 평가 본문과 태그를 저장하고 `lecture_evaluation_status`를 `COMPLETED`로 바꾼다.
- Expected Result:
  - 평가 데이터가 저장되고 같은 학기에 다시 제출할 수 없다.

### Boundary Flow: 평가 필요 없음
- Condition:
  - target 학기의 `lecture_evaluation_status`가 `null`, `SKIPPED`, `COMPLETED` 중 하나이다.
- Expected Behavior:
  - 조회 API는 해당 `evaluationStatus`와 `grades=[]`를 반환한다. row가 없거나 status가 null이면 `evaluationStatus=null`을 반환한다.

### Boundary Flow: 강의평가 건너뛰기
- Condition:
  - FE가 `PENDING` 응답으로 성적 카드를 노출했으나 사용자가 건너뛰기를 선택한다.
- Expected Behavior:
  - `POST /api/lecture-evaluations/skip`은 해당 학기가 `PENDING`인 경우에만 `lecture_evaluation_status = SKIPPED`로 변경한다.

### Boundary Flow: 일부 과목이 아직 IP
- Condition:
  - target 학기 과목 일부의 `grade = IP`이다.
- Expected Behavior:
  - `IP` 과목은 성적 카드 조회와 제출 대상 검증에서 제외한다.

## 4. Transaction / Consistency
- Transaction Start Point:
  - 포털 동기화: `SyncAcademicRecordService#sync`
  - 제출/건너뛰기: `LectureEvaluationService#submit`, `LectureEvaluationService#skip`
- Transaction End Point:
  - 포털 동기화: 변경된 수강 기록과 학기 평가 상태 flush
  - 제출: 평가 본문/태그 저장과 완료 상태 갱신
  - 건너뛰기: 학기 상태 갱신
- Atomicity Scope:
  - 제출 API는 평가 저장과 `lecture_evaluation_status = COMPLETED` 갱신을 같은 트랜잭션에서 처리한다.
  - 건너뛰기 API는 `lecture_evaluation_status = SKIPPED` 갱신을 같은 트랜잭션에서 처리한다.
- Eventual Consistency Allowed:
  - 포털 재동기화 완료 후 FE 조회 시점에는 즉시 반영되어야 한다.

## 5. API List
### 강의평가 상태 조회
- Endpoint:
  - Method: `GET`
  - Path: `/api/lecture-evaluations/required`
  - Request DTO: 없음
  - Response DTO: `LectureEvaluationRequiredResponse`
  - Authorization: JWT 인증 필요
  - Idempotency: Yes

### 강의평가 제출
- Endpoint:
  - Method: `POST`
  - Path: `/api/lecture-evaluations`
  - Request DTO: `LectureEvaluationSubmitRequest`
  - Response DTO: `MessageOnlyResponse`
  - Authorization: JWT 인증 필요
  - Idempotency: No

### 강의평가 건너뛰기
- Endpoint:
  - Method: `POST`
  - Path: `/api/lecture-evaluations/skip`
  - Request DTO: `LectureEvaluationSkipRequest`
  - Response DTO: `MessageOnlyResponse`
  - Authorization: JWT 인증 필요
  - Idempotency: No

## 6. Exception Policy
- Error Code: 신규 강의평가 대상 없음
  - Condition:
    - 제출 또는 건너뛰기 시 현재 학기가 `lecture_evaluation_status = PENDING` 상태가 아니다.
  - Handling Layer:
    - `LectureEvaluationService`
  - User Exposure:
    - 공통 에러 응답 포맷
- Error Code: 신규 강의평가 과목 불일치
  - Condition:
    - 제출 과목이 서버가 계산한 평가 대상 과목과 일치하지 않는다.
  - Handling Layer:
    - `LectureEvaluationService`
  - User Exposure:
    - 공통 에러 응답 포맷
- Error Code: 기존 `INVALID_ARGUMENT`
  - Condition:
    - review 2000자 초과, enum 파싱 실패, 요청 구조 오류
  - Handling Layer:
    - Bean Validation 또는 service validation
  - User Exposure:
    - 공통 에러 응답 포맷

## 7. Phase Checklist
- [x] Phase 1 Spec fixed
- [ ] Phase 2 Migration complete
- [ ] Phase 3 Domain model complete
- [ ] Phase 4 API/service complete
- [ ] Phase 5 Sync detection complete
- [ ] Phase 6 Documentation/tests complete

## 8. Generated File List
- Path: `docs/specs/20260620-lecture-evaluation/spec.md`
  - Description: Issue 245 범위와 도메인 규칙 정의
  - Layer: Context documentation
- Path: `docs/specs/20260620-lecture-evaluation/clarify.md`
  - Description: 사용자 결정사항과 열린 질문 기록
  - Layer: Context documentation
- Path: `docs/specs/20260620-lecture-evaluation/plan.md`
  - Description: 레이어별 구현 계획
  - Layer: Context documentation
- Path: `docs/specs/20260620-lecture-evaluation/tasks.md`
  - Description: 작업 체크리스트와 검증 로그
  - Layer: Context documentation
