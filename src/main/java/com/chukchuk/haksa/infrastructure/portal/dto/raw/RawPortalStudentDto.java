// 포털 학생 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포털에서 받은 학생 신상 및 학적 정보를 표현한다.
 *
 * @param sno 학번
 * @param studNm 학생명
 * @param univCd 단과대학 코드
 * @param univNm 단과대학명
 * @param dpmjCd 학과 코드
 * @param dpmjNm 학과명
 * @param mjorCd 전공 코드
 * @param mjorNm 전공명
 * @param the2MjorCd 복수전공 코드
 * @param the2MjorNm 복수전공명
 * @param scrgStatNm 학적 상태명
 * @param enscYear 입학 연도
 * @param enscSmrCd 입학 학기 코드
 * @param enscDvcd 입학 구분 코드
 * @param studGrde 학년
 * @param facSmrCnt 이수 학기 수
 * @param flangPassGb 외국어 인증 통과 구분
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalStudentDto(
    String sno,
    String studNm,
    String univCd,
    String univNm,
    String dpmjCd,
    String dpmjNm,
    String mjorCd,
    String mjorNm,
    String the2MjorCd,
    String the2MjorNm,
    String scrgStatNm,
    String enscYear,
    String enscSmrCd,
    String enscDvcd,
    Integer studGrde,
    Integer facSmrCnt,
    String flangPassGb) {}
