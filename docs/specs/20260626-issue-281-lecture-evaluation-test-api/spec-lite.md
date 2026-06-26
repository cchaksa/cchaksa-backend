# Issue 281: dev 전용 강의평가 상태 테스트 API

## 목적

프론트엔드 개발자가 dev 환경에서 강의평가 화면의 주요 상태를 안정적으로 재현할 수 있도록, 고정 테스트 계정의 강의평가 관련 데이터를 인증 없이 원하는 상태로 재구성하는 API를 제공한다.

## 배경

기존 `Admin Test` API는 실제 관리자 기능이라기보다 프론트 개발과 QA를 위한 dev 전용 테스트 데이터 조작 API다. 이번 작업도 같은 성격이며, 운영 profile에서는 컨트롤러가 로드되지 않아야 한다.

대상 고정 계정은 다음 값으로 고정한다.

- `user_id`: `faf9c30a-9674-4624-8855-6d0be23c749b`.
- `student_id`: `47f72b79-a3f0-4834-869b-8ba3a0cf3474`.
- `year`: `2026`.
- `semester`: `10`.

## 도메인 정리

- `semester_academic_records`는 포털 학기 성적 요약이 있을 때 생성된다.
- 해당 학기 과목이 아예 없는 상태는 일반적으로 `semester_academic_records` row도 없는 상태다.
- 따라서 "과목 없음" 테스트는 `NOT_RELEASED`가 아니라 `GET /api/lecture-evaluations/required` 응답의 `evaluationStatus=null`, `grades=[]`를 재현하는 `empty-semester` 상태로 정의한다.
- `NOT_RELEASED`는 해당 학기의 `semester_academic_records` row와 `student_courses` row가 존재하고, 대상 학기 수강 과목 성적이 모두 `IP`인 상태다.
- `PENDING`은 해당 학기 row와 성적 공개 완료 수강 과목이 있고, 강의평가 데이터는 아직 없는 상태다.
- `SKIPPED`는 `PENDING`과 같은 성적 조건에 더해 `lecture_evaluation_status=SKIPPED`이고, 해당 학기 강의평가 데이터는 없는 상태다.
- `COMPLETED`는 `PENDING`과 같은 성적 조건에 더해 `lecture_evaluation_status=COMPLETED`이고, 과목별 `course_evaluations`와 `course_evaluation_tags`가 있는 상태다.

## API 기본안

모든 API는 `POST`이고 요청 body는 받지 않는다.

- `POST /api/admin/test-lecture-evaluations/empty-semester`.
- `POST /api/admin/test-lecture-evaluations/not-released`.
- `POST /api/admin/test-lecture-evaluations/pending`.
- `POST /api/admin/test-lecture-evaluations/skipped`.
- `POST /api/admin/test-lecture-evaluations/completed`.

응답은 기존 `Admin Test` API와 동일하게 `SuccessResponse<MessageOnlyResponse>`를 사용한다.

## 데이터 구성 규칙

- 모든 API는 순행/역행 여부와 관계없이 호출 시 먼저 대상 학생의 target 학기 평가 데이터, 수강 데이터, 학기 row를 삭제한 뒤 목표 상태에 필요한 레코드를 재구성한다.
- `empty-semester`는 재구성 없이 삭제 상태를 유지해 강의평가 상태 조회가 `evaluationStatus=null`, `grades=[]`가 되게 한다.
- `not-released`, `pending`, `skipped`, `completed`는 삭제 후 대상 학기의 `semester_academic_records`와 `student_courses`를 새로 구성한다.
- 테스트 수강 과목은 새 가짜 `Course`, `Professor`, `CourseOffering`을 만들지 않고, DB에 이미 존재하는 실제 `CourseOffering` 중 `year=2026`, `semester=10`, `course_id IS NOT NULL`, `professor_id IS NOT NULL` 조건을 만족하는 row를 재사용한다.
- 조건에 맞는 실제 `CourseOffering`이 없으면 임의 데이터를 만들지 않고 실패 응답을 반환한다.
- `not-released`는 재사용한 수강 과목 성적을 `IP`로 저장한다.
- `pending`, `skipped`, `completed`는 재사용한 수강 과목 성적을 `A+` 같은 완료 성적으로 저장한다.
- `completed`는 재사용한 각 과목의 `course_id`, `professor_id` 기준으로 `course_evaluations`와 `course_evaluation_tags`를 생성한다.
- 호출 후 대상 학생 학업 캐시는 삭제한다.

## 구현 범위

- 신규 dev/test 전용 서비스와 API 메서드를 추가한다.
- 필요한 repository 삭제/조회 메서드를 추가한다.
- security public endpoint에 신규 API를 추가한다.
- static OpenAPI 문서에 신규 API와 응답을 반영한다.
- 관련 controller, service, security, OpenAPI 테스트를 추가한다.

## 제외

- 운영 profile 활성화.
- 별도 관리자 권한 체계.
- 임의 테스트 강의 또는 임의 테스트 교수 생성.
- 강의평가 통계, 조회, 수정 API.
- 프론트엔드 화면 구현.

## 완료 조건

- 5개 API가 dev/test profile에서만 로드된다.
- 5개 API가 인증 없이 호출 가능하다.
- `empty-semester` 호출 후 `GET /api/lecture-evaluations/required`는 `evaluationStatus=null`, `grades=[]`가 되는 데이터 상태를 만든다.
- `not-released` 호출 후 target 학기 row와 IP 수강 과목이 존재하고 `lecture_evaluation_status=NOT_RELEASED`가 된다.
- `pending` 호출 후 target 학기 row와 완료 성적 수강 과목이 존재하고 평가 데이터 없이 `lecture_evaluation_status=PENDING`이 된다.
- `skipped` 호출 후 target 학기 row와 완료 성적 수강 과목이 존재하고 평가 데이터 없이 `lecture_evaluation_status=SKIPPED`가 된다.
- `completed` 호출 후 target 학기 row, 완료 성적 수강 과목, 과목별 평가 데이터와 태그가 존재하고 `lecture_evaluation_status=COMPLETED`가 된다.
- 실제 존재하는 교수 연결 `CourseOffering`이 없으면 임의 데이터를 만들지 않고 실패한다.
- 관련 API가 `src/main/resources/public/openapi.yaml`에 반영된다.
- 관련 테스트와 전체 `./gradlew test`가 통과한다.
