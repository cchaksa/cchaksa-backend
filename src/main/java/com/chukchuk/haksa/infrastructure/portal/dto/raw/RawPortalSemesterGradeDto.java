// 포털 학기 성적 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포털에서 받은 학기별 성적 요약을 표현한다.
 *
 * @param cretGainYear 이수 연도
 * @param cretSmrCd 이수 학기 코드
 * @param gainPoint 취득 학점
 * @param applPoint 신청 학점
 * @param gainAvmk 학기 평점
 * @param gainTavgPont 학기 백분위
 * @param dpmjOrdp 학과 석차
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalSemesterGradeDto(
    String cretGainYear,
    String cretSmrCd,
    String gainPoint,
    String applPoint,
    String gainAvmk,
    String gainTavgPont,
    String dpmjOrdp) {}
