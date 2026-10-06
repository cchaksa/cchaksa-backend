// 포털 학기별 수강 과목 원본 데이터를 전달한다.

package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 포털에서 받은 학기와 수강 과목 목록을 표현한다.
 *
 * @param semester 학기 식별값
 * @param courses 해당 학기의 수강 과목 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalSemesterDto(String semester, List<RawPortalCourseDto> courses) {}
