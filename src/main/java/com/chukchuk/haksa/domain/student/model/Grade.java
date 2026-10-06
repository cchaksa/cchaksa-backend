package com.chukchuk.haksa.domain.student.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** 수강 과목의 원본 성적 문자열과 표준 성적 유형을 함께 보관한다. */
@Embeddable
public class Grade {

  @Convert(converter = GradeTypeConverter.class)
  @Column(name = "grade")
  private GradeType value;

  /** JPA가 수강 성적 값 객체를 복원할 때 사용하는 생성자다. */
  protected Grade() {} // JPA를 위한 기본 생성자

  /**
   * 확정된 성적 유형을 수강 성적 값 객체로 생성한다.
   *
   * @param value 성적 유형
   */
  public Grade(GradeType value) {
    this.value = value;
  }

  /**
   * 아직 확정되지 않은 수강 성적을 생성한다.
   *
   * @return 미확정 상태의 성적 값
   */
  public static Grade createInProgress() {
    return new Grade(GradeType.IP);
  }

  public GradeType getValue() {
    return value;
  }

  /**
   * 성적 처리가 완료된 상태인지 확인한다.
   *
   * @return 성적이 {@code IP}가 아니면 {@code true}
   */
  public boolean isCompleted() {
    return value != GradeType.IP;
  }

  /**
   * 학점을 취득한 통과 성적인지 확인한다.
   *
   * @return 성적 분류가 통과 상태이면 {@code true}
   */
  public boolean isPassingGrade() {
    return value != GradeType.F && value != GradeType.NP && value != GradeType.IP;
  }

  public double getGradePoint() {
    return value.getGradePoint();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Grade g)) {
      return false;
    }
    return value == g.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
