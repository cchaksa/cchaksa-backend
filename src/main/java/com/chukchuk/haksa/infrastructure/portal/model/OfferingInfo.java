package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털에서 조회한 특정 연도·학기의 개설 과목 정보를 표현한다.
 *
 * @param courseCode 과목 코드
 * @param year 연도
 * @param semester 개설 학기
 * @param classSection 분반
 * @param professorName 담당 교수 이름
 * @param scheduleSummary 강의 시간·장소 요약
 * @param points 학점
 * @param hostDepartment 주관 학과
 * @param facultyDivisionName 학부 구분 이름
 * @param subjectEstablishmentSemester 과목 개설 학기 구분
 * @param areaCode 현재 교양 영역 코드
 * @param originalAreaCode 변경 전 교양 영역 코드
 * @param evaluationType 성적 평가 방식
 * @param isVideoLecture 영상 강의 여부
 */
public record OfferingInfo(
    String courseCode,
    int year,
    int semester,
    String classSection,
    String professorName,
    String scheduleSummary,
    Integer points,
    String hostDepartment,
    String facultyDivisionName,
    Integer subjectEstablishmentSemester,
    Integer areaCode,
    Integer originalAreaCode,
    String evaluationType,
    Boolean isVideoLecture) {}
