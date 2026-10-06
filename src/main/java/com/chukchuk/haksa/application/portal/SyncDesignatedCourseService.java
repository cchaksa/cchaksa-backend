// 포털 지정과목 스냅샷을 학생별로 최신 상태에 맞춰 저장한다.

package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseSnapshot;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생 행을 잠근 뒤 지정과목 스냅샷을 원자적으로 교체한다. */
@Service
@RequiredArgsConstructor
public class SyncDesignatedCourseService {

  private final StudentRepository studentRepository;
  private final StudentDesignatedCourseRepository designatedCourseRepository;
  private final AcademicCache academicCache;

  /**
   * 수신된 지정과목 스냅샷을 버전 순서에 따라 저장한다.
   *
   * @param userId 스냅샷을 저장할 사용자의 식별자
   * @param snapshot 저장할 지정과목 스냅샷
   * @param snapshotVersion 스냅샷을 생성한 작업의 시각
   * @throws IllegalArgumentException 수신된 스냅샷의 버전이 없는 경우
   * @throws EntityNotFoundException 사용자에게 연결된 학생이 없는 경우
   */
  @Transactional
  public void sync(UUID userId, DesignatedCourseSnapshot snapshot, Instant snapshotVersion) {
    if (!snapshot.received()) {
      return;
    }
    if (snapshotVersion == null) {
      throw new IllegalArgumentException("지정과목 스냅샷 버전이 없습니다.");
    }

    Student student =
        studentRepository
            .findForUpdateByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
    if (student.getUser() != null && Boolean.TRUE.equals(student.getUser().getIsDeleted())) {
      return;
    }
    if (!student.canApplyDesignatedCourseSnapshot(snapshotVersion)) {
      return;
    }

    designatedCourseRepository.deleteAllByStudentId(student.getId());
    if (!snapshot.courses().isEmpty()) {
      designatedCourseRepository.saveAll(
          snapshot.courses().stream()
              .map(course -> new StudentDesignatedCourse(student, course))
              .toList());
    }
    student.updateDesignatedCourseSnapshotVersion(snapshotVersion);
    academicCache.deleteAllByStudentId(student.getId());
  }
}
