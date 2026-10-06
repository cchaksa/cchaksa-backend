package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털에서 조회한 학생의 수강 과목·성적·재수강 정보를 표현한다.
 *
 * @param code 과목 코드
 * @param name 과목 이름
 * @param professor 담당 교수 이름
 * @param department 개설 학과 이름
 * @param credits 이수 학점
 * @param grade 취득 성적
 * @param isRetake 재수강 여부
 * @param schedule 강의 시간·장소 요약
 * @param areaType 교양 영역 유형
 * @param areaCode 현재 교양 영역 코드
 * @param originalAreaCode 변경 전 교양 영역 코드
 * @param establishmentSemester 과목 개설 학기 구분
 * @param originalScore 원점수
 * @param isRetakeDeleted 재수강으로 대체된 성적의 제외 여부
 */
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
    boolean isRetakeDeleted) {}
