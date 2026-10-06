// 포털 전체 성적 요약 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포털에서 받은 전체 성적 요약을 표현한다.
 *
 * @param gainPoint 취득 학점
 * @param applPoint 신청 학점
 * @param gainAvmk 누적 평점
 * @param gainTavgPont 누적 백분위
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalGradeSummaryDto(
    String gainPoint, String applPoint, String gainAvmk, String gainTavgPont) {}
