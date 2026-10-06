package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털에서 조회한 학생의 소속·학적·입학 및 졸업 인증 정보를 표현한다.
 *
 * @param studentCode 학번
 * @param name 이름
 * @param college 소속 단과대학
 * @param department 소속 학과
 * @param major 주전공
 * @param secondaryMajor 복수전공
 * @param status 상태
 * @param admission 입학 연도·학기와 입학 유형
 * @param academic 현재 학년과 누적 이수 현황
 * @param languageCertFulfilled 어학 인증 충족 여부
 */
public record PortalStudentInfo(
    String studentCode,
    String name,
    CodeName college,
    CodeName department,
    CodeName major,
    CodeName secondaryMajor,
    String status,
    AdmissionInfo admission,
    PortalAcademicInfo academic,
    Boolean languageCertFulfilled) {}
