package com.chukchuk.haksa.domain.department.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학과 area 요건 도메인 상태를 표현한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_area_requirements")
public class DepartmentAreaRequirement extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "total_elective_courses")
  private Integer totalElectiveCourses;

  @Column(name = "required_elective_courses")
  private Integer requiredElectiveCourses;

  @Column(name = "admission_year", nullable = false)
  private Integer admissionYear;

  @Column(name = "required_credits", nullable = false)
  private Integer requiredCredits;

  @Column(name = "description")
  private String description;

  @Column(name = "area_type", nullable = false)
  private String areaType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;
}
