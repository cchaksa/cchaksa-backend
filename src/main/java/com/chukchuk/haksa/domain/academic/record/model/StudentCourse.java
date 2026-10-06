package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 학생의 수강 과목과 성적, 재수강 여부를 보관한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "student_courses")
public class StudentCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  @Column(name = "grade")
  private Grade grade;

  @Column(name = "points")
  private Integer points;

  @Column(name = "is_retake")
  private Boolean isRetake;

  @Column(name = "original_score")
  private Integer originalScore;

  @CreatedDate
  @Column(name = "created_at")
  private Instant createdAt;

  @Column(nullable = false)
  private boolean isRetakeDeleted = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offering_id", nullable = false)
  private CourseOffering offering;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  /**
   * 학생과 개설 과목에 취득 성적을 연결한 수강 내역을 생성한다.
   *
   * @param student 과목을 수강한 학생
   * @param offering 수강한 강의 개설 정보
   * @param grade 취득 성적
   * @param points 과목 학점
   * @param isRetake 재수강 여부
   * @param originalScore 원점수
   * @param isRetakeDeleted 재수강으로 기존 성적이 삭제된 수강 기록인지 여부
   */
  public StudentCourse(
      Student student,
      CourseOffering offering,
      Grade grade,
      Integer points,
      Boolean isRetake,
      Integer originalScore,
      boolean isRetakeDeleted) {
    this.student = student;
    this.offering = offering;
    this.grade = grade;
    this.points = points;
    this.isRetake = isRetake;
    this.originalScore = originalScore;
    this.isRetakeDeleted = isRetakeDeleted;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  public void setRetakeDeleted(Boolean retakeDeleted) {
    this.isRetakeDeleted = retakeDeleted;
  }

  /**
   * 저장된 성적·학점·재수강 상태가 포털 수강 정보와 다른지 확인한다.
   *
   * @param pe 비교할 포털 수강 정보
   * @return 비교 대상 필드 중 하나라도 다르면 {@code true}
   */
  public boolean isDifferentFrom(CourseEnrollment pe) {
    return !Objects.equals(this.grade, pe.getGrade())
        || !Objects.equals(
            this.originalScore,
            pe.getOriginalScore() != null ? pe.getOriginalScore().intValue() : null)
        || !Objects.equals(this.isRetake, pe.isRetake())
        || !Objects.equals(this.points, pe.getPoints())
        || this.isRetakeDeleted != pe.isRetakeDeleted();
  }

  /**
   * 포털 수강 정보로 성적·학점·재수강 상태를 갱신한다.
   *
   * @param pe 적용할 포털 수강 정보
   */
  public void updateFromPortal(CourseEnrollment pe) {
    this.grade = pe.getGrade();
    this.originalScore = pe.getOriginalScore() != null ? pe.getOriginalScore().intValue() : null;
    this.points = pe.getPoints();
    this.isRetake = pe.isRetake();
    this.isRetakeDeleted = pe.isRetakeDeleted();
  }
}
