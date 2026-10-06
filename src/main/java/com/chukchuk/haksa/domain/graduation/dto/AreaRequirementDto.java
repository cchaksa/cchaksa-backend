package com.chukchuk.haksa.domain.graduation.dto;

/**
 * 졸업 영역별 필수 학점과 선택 과목 수 기준을 전달한다.
 *
 * @param areaType 졸업 요건을 구분하는 영역
 * @param requiredCredits required 학점
 * @param requiredElectiveCourses 영역에서 이수해야 하는 최소 선택 과목 수
 * @param totalElectiveCourses 영역에 등록된 전체 선택 과목 수
 */
public record AreaRequirementDto(
    String areaType,
    int requiredCredits,
    Integer requiredElectiveCourses,
    Integer totalElectiveCourses) {}
