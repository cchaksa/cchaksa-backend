# Checklist

- [x] `student_graduation_progress`가 있는 학생 탈퇴 실패를 재현하는 통합 테스트를 추가한다.
- [x] 실패 테스트가 기존 코드에서 실패하는지 확인한다.
- [x] `StudentGraduationProgressRepository.deleteByStudentId(UUID studentId)`를 추가한다.
- [x] `StudentDeletionService`가 학생 삭제 전에 졸업 진행 데이터를 삭제하도록 수정한다.
- [x] 탈퇴 통합 테스트와 졸업 진행 관련 테스트를 실행한다.
- [x] 변경사항을 커밋한다.
