package com.chukchuk.haksa.domain.lectureevaluations.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과목 evaluation tag 도메인 상태를 표현한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_evaluation_tags",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_course_evaluation_tags_evaluation_tag",
          columnNames = {"course_evaluation_id", "tag"})
    },
    indexes = {
      @Index(name = "idx_course_evaluation_tags_evaluation", columnList = "course_evaluation_id")
    })
public class CourseEvaluationTag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_evaluation_id", nullable = false)
  private CourseEvaluation courseEvaluation;

  @Enumerated(EnumType.STRING)
  @Column(name = "tag", nullable = false, length = 64)
  private LectureEvaluationTag tag;

  /**
   * 강의평가에 사용자가 선택한 태그를 연결한다.
   *
   * @param courseEvaluation 태그가 속한 강의평가
   * @param tag 사용자가 선택한 평가 태그
   */
  public CourseEvaluationTag(CourseEvaluation courseEvaluation, LectureEvaluationTag tag) {
    this.courseEvaluation = courseEvaluation;
    this.tag = tag;
  }
}
