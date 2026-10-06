package com.chukchuk.haksa.domain.graduation.policy;

/**
 * 졸업 요건 계산에 사용할 주전공과 복수전공 식별 결과를 표현한다.
 *
 * @param primaryMajorId 주전공 식별자
 * @param secondaryMajorId 복수전공 식별자
 */
public record MajorResolutionResult(Long primaryMajorId, Long secondaryMajorId) {}
