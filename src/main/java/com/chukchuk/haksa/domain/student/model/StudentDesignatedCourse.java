// 학생별 포털 지정과목 원본 한 행을 저장한다.

package com.chukchuk.haksa.domain.student.model;

import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학생에게 내려온 지정과목 원본 필드와 배열 순서를 보관한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "student_designated_courses",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_student_designated_courses_student_order",
            columnNames = {"student_id", "source_order"}))
public class StudentDesignatedCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @Column(name = "source_order", nullable = false)
  private int sourceOrder;

  @Column(name = "org_cls_cd")
  private String orgClsCd;

  @Column(name = "subjt_cd")
  private String subjtCd;

  @Column(name = "subjt_nm")
  private String subjtNm;

  @Column(name = "point")
  private Integer point;

  @Column(name = "precp_resn_cd")
  private String precpResnCd;

  @Column(name = "cret_gain_year")
  private Integer cretGainYear;

  @Column(name = "cret_smr_nm")
  private String cretSmrNm;

  @Column(name = "sno")
  private String sno;

  /**
   * 내부 지정과목 행을 학생과 연결해 저장할 엔티티로 변환한다.
   *
   * @param student 지정과목을 소유한 학생
   * @param course 내부 지정과목 값
   */
  public StudentDesignatedCourse(Student student, DesignatedCourseData course) {
    this.student = student;
    this.sourceOrder = course.sourceOrder();
    this.orgClsCd = course.orgClsCd();
    this.subjtCd = course.subjtCd();
    this.subjtNm = course.subjtNm();
    this.point = course.point();
    this.precpResnCd = course.precpResnCd();
    this.cretGainYear = course.cretGainYear();
    this.cretSmrNm = course.cretSmrNm();
    this.sno = course.sno();
  }
}
