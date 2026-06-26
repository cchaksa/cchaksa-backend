// 학생별 졸업 진행 상태를 조회하고 저장하는 JPA Repository
package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentGraduationProgressRepository extends JpaRepository<StudentGraduationProgress, UUID> {
    Optional<StudentGraduationProgress> findByStudentId(UUID studentId);

    void deleteByStudentId(UUID studentId);
}
