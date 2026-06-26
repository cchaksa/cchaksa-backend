# 회원 탈퇴 시 졸업 진행 데이터 삭제 누락 수정

## 배경

회원 탈퇴 시 `students` 삭제 전에 학생을 참조하는 하위 데이터를 삭제한다.
운영 DB 확인 결과 `student_graduation_progress.student_id`가 `students.student_id`를 FK로 참조하지만, 현재 탈퇴 로직은 해당 테이블을 삭제하지 않는다.

## 목표

회원 탈퇴 시 `student_graduation_progress` 행도 같은 트랜잭션에서 삭제해 졸업 진행 데이터가 있는 사용자의 탈퇴 실패를 방지한다.

## 범위

- `StudentGraduationProgressRepository`에 학생 ID 기준 삭제 메서드를 추가한다.
- `StudentDeletionService`에서 학생 삭제 전에 졸업 진행 데이터를 삭제한다.
- 회원 탈퇴 통합 테스트에 졸업 진행 데이터가 있는 학생 케이스를 포함한다.

## 제외

- API 응답 형식 변경.
- DB schema 또는 Flyway migration 변경.
- 학생 학업 데이터 초기화인 `StudentService.resetBy(...)` 동작 변경.

## 검증

- `./gradlew test --tests "*UserServiceIntegrationTest"`
- `./gradlew test --tests "*StudentGraduationProgressServiceTests" --tests "*UserServiceIntegrationTest"`
