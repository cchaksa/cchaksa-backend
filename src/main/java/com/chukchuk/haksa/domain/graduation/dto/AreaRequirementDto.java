package com.chukchuk.haksa.domain.graduation.dto;

public record AreaRequirementDto(
        String areaType,
        int requiredCredits,
        Integer requiredElectiveCourses,
        Integer totalElectiveCourses
) {}