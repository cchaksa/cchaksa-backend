package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEvaluationTagRepository extends JpaRepository<CourseEvaluationTag, Long> {
}
