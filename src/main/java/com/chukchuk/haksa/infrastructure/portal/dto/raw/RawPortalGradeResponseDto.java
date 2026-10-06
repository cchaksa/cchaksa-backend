// 포털 성적 조회 원본 응답을 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 포털에서 받은 학기별 성적과 전체 성적 요약을 표현한다.
 *
 * @param listSmrCretSumTabYearSmr 학기별 성적 목록
 * @param selectSmrCretSumTabSjTotal 전체 성적 요약
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalGradeResponseDto(
    List<RawPortalSemesterGradeDto> listSmrCretSumTabYearSmr,
    RawPortalGradeSummaryDto selectSmrCretSumTabSjTotal) {}
