package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 학생의 입학 연도·학기와 입학 유형을 표현한다.
 *
 * @param year 연도
 * @param semester 입학 학기
 * @param type 정시·편입 등 입학 유형
 */
public record AdmissionInfo(int year, int semester, String type // 예: 정시, 편입 등
    ) {}
