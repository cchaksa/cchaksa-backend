package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalGradeResponseDTO(
        List<RawPortalSemesterGradeDTO> listSmrCretSumTabYearSmr,
        RawPortalGradeSummaryDTO selectSmrCretSumTabSjTotal
) {}
