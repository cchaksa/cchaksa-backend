package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 학기 성적의 석차와 석차 산정 대상 인원을 표현한다.
 *
 * @param rank 학생의 석차
 * @param total 석차 산정 대상 학생 수
 */
public record Ranking(int rank, int total) {}
