package com.chukchuk.haksa.infrastructure.portal.model;

import lombok.Data;

/** 포털 과목을 내부 개설 과목과 매칭할 때 사용하는 과목·성적 정보를 표현한다. */
@Data
public class PortalCourseInfo {
  private String code; // courseCode
  private String name;
  private int credits;
  private String professor;
  private String schedule;
  private int establishmentSemester;
  private boolean isRetake;
  private Double originalScore;
  private String grade;
  private boolean isRetakeDeleted;
  // 생성자, getter 생략 가능
}
