package com.chukchuk.haksa.domain.course.dto;

/**
 * 포털 과목 정보로 개설 강의를 생성할 때 필요한 값을 전달한다.
 *
 * @param courseId 과목 식별자
 * @param year 연도
 * @param semester 대상 학기
 * @param classSection 분반
 * @param professorId 교수 식별자
 * @param departmentId 학과 식별자
 * @param scheduleSummary 강의 시간과 장소를 요약한 문자열
 * @param evaluationType 성적 평가 방식 이름
 * @param isVideoLecture 온라인 영상 강의인지 여부
 * @param subjectEstablishmentSemester 과목이 개설되는 기준 학기
 * @param facultyDivisionName faculty division 이름
 * @param areaCode 정규화된 교양 영역 코드
 * @param originalAreaCode 포털에서 받은 원본 교양 영역 코드
 * @param points 학점
 * @param hostDepartment 주관 학과
 */
public record CreateOfferingCommand(
    Long courseId,
    Integer year,
    Integer semester,
    String classSection,
    Long professorId,
    Long departmentId,
    String scheduleSummary,
    String evaluationType, // EvaluationType name (ex: "ABSOLUTE")
    Boolean isVideoLecture,
    Integer subjectEstablishmentSemester,
    String facultyDivisionName,
    Integer areaCode,
    Integer originalAreaCode,
    Integer points,
    String hostDepartment) {}
