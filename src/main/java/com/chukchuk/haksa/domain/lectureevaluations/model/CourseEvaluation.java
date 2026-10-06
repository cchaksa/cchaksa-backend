package com.chukchuk.haksa.domain.lectureevaluations.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과목 evaluation 도메인 상태를 표현한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_evaluations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_course_evaluations_student_semester_course_professor",
          columnNames = {"student_id", "year", "semester", "course_id", "professor_id"})
    },
    indexes = {
      @Index(
          name = "idx_course_evaluations_course_professor",
          columnList = "course_id, professor_id")
    })
public class CourseEvaluation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "professor_id", nullable = false)
  private Professor professor;

  @Column(name = "year", nullable = false)
  private Integer year;

  @Column(name = "semester", nullable = false)
  private Integer semester;

  @Column(name = "review", length = 2000)
  private String review;

  @OneToMany(mappedBy = "courseEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CourseEvaluationTag> tags = new ArrayList<>();

  /**
   * 학생의 과목 후기와 선택 태그를 연결한 강의평가를 생성한다.
   *
   * @param student 기록의 소유 학생
   * @param course 연결할 수강 과목
   * @param professor 교수
   * @param year 연도
   * @param semester 대상 학기
   * @param review 강의평가에 저장할 서술형 후기
   * @param selectedTags 강의평가에 연결할 선택 태그 집합
   */
  public CourseEvaluation(
      Student student,
      Course course,
      Professor professor,
      Integer year,
      Integer semester,
      String review,
      List<LectureEvaluationTag> selectedTags) {
    this.student = student;
    this.course = course;
    this.professor = professor;
    this.year = year;
    this.semester = semester;
    this.review = review;
    setTags(selectedTags);
  }

  private void setTags(List<LectureEvaluationTag> selectedTags) {
    if (selectedTags == null) {
      return;
    }
    selectedTags.stream()
        .distinct()
        .map(tag -> new CourseEvaluationTag(this, tag))
        .forEach(this.tags::add);
  }
}
