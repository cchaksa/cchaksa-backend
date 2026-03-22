package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalStudentDTO(
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
        Integer facSmrCnt
) {}
