package com.chukchuk.haksa.domain.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseInternalDto {
    private Long offeringId;
    private String areaType;         // FacultyDivision은 나중에 파싱
    private Integer credits;
    private String grade;
    private String courseName;
    private Integer semester;
    private Integer year;
    private String courseCode;
    private Integer originalScore;
}

