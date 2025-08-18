package com.chukchuk.haksa.infrastructure.portal.model;

public record CourseInfo(
        String code,
        String name,
        String professor,
        String department,
        Integer credits,
        String grade,
        boolean isRetake,
        String schedule,
        String areaType,
        Integer areaCode,
        Integer originalAreaCode,
        Integer establishmentSemester,
        Double originalScore,
        boolean isRetakeDeleted
) {}