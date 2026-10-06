// 포털에서 받은 지정과목 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 포털 지정과목 한 행의 원본 정보를 표현한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalDesignatedCourseDto(
    String orgClsCd,
    String subjtCd,
    String subjtNm,
    String point,
    String precpResnCd,
    String cretGainYear,
    String cretSmrNm,
    String sno) {}
