package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalSemesterGradeDTO(
        String cretGainYear,
        String cretSmrCd,
        String gainPoint,
        String applPoint,
        String gainAvmk,
        String gainTavgPont,
        String dpmjOrdp
) {}
