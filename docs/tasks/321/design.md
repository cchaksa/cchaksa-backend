# 회원 탈퇴 강의평가 보존 익명화 설계

## 연결 이슈

- GitHub Issue [#321](https://github.com/cchaksa/cchaksa-backend/issues/321)

## 문제

회원 탈퇴는 학생 행을 삭제한다. 강의평가가 존재하면 `course_evaluations.student_id` 외래 키가 학생 삭제를 막아 탈퇴 트랜잭션이 실패한다.

## 처리 정책

- 강의평가와 강의평가 태그는 삭제하지 않는다.
- User와 Student 행은 남기되 개인 식별정보를 익명화한다.
- User는 `is_deleted=true` 및 `deleted_at`을 기록하고 이메일과 프로필 정보를 제거한다.
- Student는 이름을 `탈퇴한 사용자입니다.`로, 학번을 `deleted_{UUID}`로 바꾼다.
- 학적, 수강, 졸업진행 데이터는 기존처럼 삭제한다.
- 소셜 계정, Refresh Token, 인증 캐시, 학적 캐시는 탈퇴 시 정리한다.
- 캐시를 비운 뒤 재사용되는 access token은 JWT 필터가 `UserDetails.isEnabled()`를 확인해 차단한다.
- 재가입은 새 User와 새 Student를 생성하며 과거 익명 평가와 병합하지 않는다.

## 설계 근거

`students.user_id`와 `course_evaluations.student_id`가 모두 NOT NULL 외래 키다. Student를 유지하려면 연결된 User도 유지해야 한다. 따라서 두 행을 함께 익명화하면 강의평가의 참조 무결성을 유지하면서 소셜 식별자, 이메일, 학번, 이름을 제거할 수 있다.

## 변경 범위

- User 탈퇴 상태 전환 메서드를 추가한다.
- Student 삭제 서비스는 연관 학적 데이터 정리 후 Student 익명화로 동작을 바꾼다.
- Refresh Token을 사용자 ID 기준으로 삭제한다.
- User-Student 관계의 REMOVE cascade를 제거한다.
- 공개 API와 DB 스키마는 변경하지 않는다. `is_deleted`, `deleted_at`, nullable 이메일 컬럼은 이미 존재한다.

## 검증

- 강의평가와 태그가 있는 사용자도 탈퇴에 성공한다.
- User와 Student는 익명화되고 학적 데이터와 인증 정보는 제거된다.
- 강의평가의 Student ID와 태그는 유지된다.
- 재가입은 이전 익명 Student가 아닌 새 Student를 사용한다.
- 탈퇴 User의 기존 access token은 401로 거부된다.
- `./gradlew test --stacktrace --no-daemon`을 Java 17로 실행한다.
