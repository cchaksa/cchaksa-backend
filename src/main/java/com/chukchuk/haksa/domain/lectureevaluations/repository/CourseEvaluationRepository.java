package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {
}
