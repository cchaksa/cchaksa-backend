package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털 분류 항목의 코드와 표시 이름을 함께 표현한다.
 *
 * @param code 포털 분류 코드
 * @param name 사용자에게 표시할 분류 이름
 */
public record CodeName(String code, String name) {}
