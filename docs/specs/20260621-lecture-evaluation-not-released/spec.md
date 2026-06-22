# Issue 271: 강의평가 성적 미공개 상태 추가

## 1. Feature Overview
- Purpose:
  - 수강 내역은 있으나 성적이 아직 `IP`인 학기를 `null`이 아닌 명시 상태로 표현한다.
- Scope:
  - In:
    - `LectureEvaluationStatus.NOT_RELEASED` 추가
    - 포털 동기화 중 성적 미공개 학기 상태 저장
    - `NOT_RELEASED -> PENDING` 상태 전환
    - 기존 `NULL` 데이터의 가능한 상태 초기화 migration
    - 조회 API/OpenAPI 문서 enum 갱신
  - Out:
    - FE 화면 구현
    - 과거 학기 별도 진입점
    - 상태 수동 세팅 API

## 2. Domain Rules
- `NOT_RELEASED`는 같은 학생/연도/학기에 수강 내역이 있지만 평가 대상 과목 성적이 모두 `IP`인 상태이다.
- `PENDING`은 하나 이상의 평가 대상 과목 성적이 `IP`에서 실제 성적으로 변경된 상태이다.
- `SKIPPED`와 `COMPLETED`는 포털 재동기화로 `NOT_RELEASED` 또는 `PENDING`으로 되돌리지 않는다.
- `null`은 해당 학기 수강 내역이 없거나 강의평가 상태 판단 대상이 아닌 상태로 남긴다.
- 조회 API는 target 학기의 `evaluationStatus`를 그대로 반환한다.
- `grades`는 기존 정책대로 `PENDING`일 때만 반환한다.

## 3. Data Migration Policy
- `lecture_evaluation_status IS NULL`인 `semester_academic_records`만 초기화한다.
- 같은 학생/연도/학기의 `student_courses`가 없으면 `NULL` 유지.
- 같은 학생/연도/학기의 `student_courses` 중 하나 이상 `grade <> 'IP'`이면 `PENDING`.
- 같은 학생/연도/학기의 `student_courses`가 있고 모든 `grade`가 `IP` 또는 `NULL`이면 `NOT_RELEASED`.
- 기존 `PENDING`, `SKIPPED`, `COMPLETED`는 덮어쓰지 않는다.

## 4. API Contract
- `GET /api/lecture-evaluations/required`
  - `evaluationStatus` enum에 `NOT_RELEASED`를 추가한다.
  - `NOT_RELEASED` 응답은 `grades=[]`이다.
- `POST /api/lecture-evaluations`
  - 기존과 동일하게 `PENDING` 상태에서만 허용한다.
- `POST /api/lecture-evaluations/skip`
  - 기존과 동일하게 `PENDING` 상태에서만 허용한다.

## 5. Acceptance Criteria
- `LectureEvaluationStatus`에 `NOT_RELEASED`가 존재한다.
- 포털 동기화에서 미공개 학기가 `NOT_RELEASED`로 저장된다.
- 같은 학기에서 성적 공개가 감지되면 `NOT_RELEASED -> PENDING`으로 전환된다.
- `SKIPPED`, `COMPLETED`는 동기화로 변경되지 않는다.
- Flyway migration이 기존 null 데이터를 `PENDING`/`NOT_RELEASED`로 초기화한다.
- OpenAPI와 테스트가 새 상태값을 반영한다.
