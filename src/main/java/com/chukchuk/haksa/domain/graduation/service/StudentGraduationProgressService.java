// 학생 졸업 진행 상태 중 외국어 인증 정보를 동기화하고 조회하는 서비스
package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGraduationProgressService {

    private final StudentGraduationProgressRepository repository;
    private final AcademicCache academicCache;

    @Transactional
    public void syncLanguageCert(Student student, Boolean languageCertFulfilled, Instant checkedAt) {
        if (languageCertFulfilled == null) {
            return;
        }

        UUID studentId = student.getId();
        StudentGraduationProgress progress = repository.findByStudentId(studentId)
                .map(existing -> {
                    existing.updateLanguageCert(languageCertFulfilled, checkedAt);
                    return existing;
                })
                .orElseGet(() -> StudentGraduationProgress.createForLanguageCert(
                        student,
                        languageCertFulfilled,
                        checkedAt
                ));

        repository.save(progress);
        academicCache.deleteAllByStudentId(studentId);
    }

    public Optional<Boolean> getLanguageCertFulfilled(UUID studentId) {
        return repository.findByStudentId(studentId)
                .map(StudentGraduationProgress::getLanguageCertFulfilled);
    }
}
