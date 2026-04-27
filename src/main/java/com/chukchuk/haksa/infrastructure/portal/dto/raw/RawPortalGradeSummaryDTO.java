package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalGradeSummaryDTO(
        String gainPoint,
        String applPoint,
        String gainAvmk,
        String gainTavgPont
) {}
