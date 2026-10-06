// 학생 졸업 진행 상태 중 외국어 인증 정보를 동기화하고 조회하는 서비스

package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생의 외국어 인증 충족 여부를 저장하고 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGraduationProgressService {

  private final StudentGraduationProgressRepository repository;
  private final AcademicCache academicCache;

  /**
   * 학생의 어학 인증 충족 상태를 동기화한다.
   *
   * @param student 기록의 소유 학생
   * @param languageCertFulfilled 어학 인증 충족 여부
   */
  @Transactional
  public void syncLanguageCert(Student student, Boolean languageCertFulfilled) {
    if (languageCertFulfilled == null) {
      return;
    }

    UUID studentId = student.getId();
    StudentGraduationProgress progress =
        repository
            .findByStudentId(studentId)
            .map(
                existing -> {
                  existing.updateLanguageCert(languageCertFulfilled);
                  return existing;
                })
            .orElseGet(
                () ->
                    StudentGraduationProgress.createForLanguageCert(
                        student, languageCertFulfilled));

    repository.save(progress);
    academicCache.deleteAllByStudentId(studentId);
  }

  /**
   * 학생의 외국어 인증 충족 여부를 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 저장된 충족 여부가 있으면 포함한 선택값
   */
  public Optional<Boolean> getLanguageCertFulfilled(UUID studentId) {
    return repository
        .findByStudentId(studentId)
        .map(StudentGraduationProgress::getLanguageCertFulfilled);
  }
}
