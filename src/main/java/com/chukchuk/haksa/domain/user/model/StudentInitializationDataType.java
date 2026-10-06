package com.chukchuk.haksa.domain.user.model;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import lombok.Builder;
import lombok.Getter;

/** 포털 학생 정보를 학생 엔티티로 초기화할 때 필요한 학적·전공 정보를 보관한다. */
@Builder
@Getter
public class StudentInitializationDataType {
  private String studentCode;
  private String name;
  private Department department; // 학과 객체
  private Department major; // 전공 객체 (nullable)
  private Department secondaryMajor; // 복수 전공 객체 (nullable)
  private int admissionYear;
  private int semesterEnrolled;
  private boolean isTransferStudent;
  private boolean isGraduated;
  private StudentStatus status;
  private int gradeLevel;
  private int completedSemesters;
  private String admissionType;
}
