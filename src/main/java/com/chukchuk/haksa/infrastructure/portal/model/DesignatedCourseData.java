// 포털 지정과목을 내부 동기화 모델로 전달한다.

package com.chukchuk.haksa.infrastructure.portal.model;

/** 저장과 동기화에 사용하는 지정과목 원본 한 행을 표현한다. */
public record DesignatedCourseData(
    String orgClsCd,
    String subjtCd,
    String subjtNm,
    Integer point,
    String precpResnCd,
    Integer cretGainYear,
    String cretSmrNm,
    String sno,
    int sourceOrder) {}
