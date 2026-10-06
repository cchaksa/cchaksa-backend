// 포털 수강 과목 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;

/**
 * 포털에서 받은 수강 과목 원본 정보를 표현한다.
 *
 * @param subjtCd 학수번호
 * @param subjtNm 과목명
 * @param ltrPrfsNm 교수명
 * @param estbDpmjNm 개설 학과명
 * @param point 학점
 * @param gainPoint 취득 학점
 * @param cretGrdCd 성적 코드
 * @param refacYearSmr 재수강 판별 식별자
 * @param timtSmryCn 시간표 요약
 * @param facDvnm 이수 구분명
 * @param cltTerrNm 교양 영역명
 * @param cltTerrCd 교양 영역 코드
 * @param subjtEstbSmrCd 개설 학기 코드
 * @param subjtEstbYearSmr 개설 연도·학기
 * @param diclNo 분반 번호
 * @param gainPont 원점수
 * @param cretDelCd 재수강 삭제 코드
 * @param cretDelNm 재수강 삭제 표시명
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalCourseDto(
    String subjtCd,
    String subjtNm,
    String ltrPrfsNm,
    String estbDpmjNm,
    Integer point,
    Integer gainPoint,
    String cretGrdCd,
    String refacYearSmr,
    String timtSmryCn,
    String facDvnm,
    String cltTerrNm,
    String cltTerrCd,
    String subjtEstbSmrCd,
    String subjtEstbYearSmr,
    String diclNo,
    String gainPont,
    Optional<String> cretDelCd,
    Optional<String> cretDelNm) {}
