# Context Notes

- 운영 DB에서 `19012037` 학생의 `student_graduation_progress` 행이 1개 확인됐다.
- 운영 DB의 `students` 참조 FK는 `semester_academic_records`, `student_academic_records`, `student_courses`, `student_graduation_progress` 네 개다.
- 기존 `StudentDeletionService`는 앞의 세 테이블만 삭제하고 `student_graduation_progress`는 삭제하지 않는다.
- 이번 변경은 회원 탈퇴 경로만 수정한다. `StudentService.resetBy(...)`는 학업 데이터 초기화 용도라 범위에서 제외한다.
- `.codex/config.toml`의 기존 변경은 이번 작업과 무관하므로 건드리지 않는다.
- RED 확인 결과 `UserServiceIntegrationTest`의 기존 탈퇴 테스트가 `student_graduation_progress` FK 제약 위반으로 실패했다.
- GREEN 확인 결과 `UserServiceIntegrationTest`와 `StudentGraduationProgressServiceTests`가 통과했다.
