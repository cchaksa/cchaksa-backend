package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import java.util.Optional;

public record RawPortalCourseDTO(
        String subjtCd,
        String subjtNm,
        String ltrPrfsNm,
        String estbDpmjNm,
        Integer point,
        String cretGrdCd,
        String refacYearSmr, // 기존 재수강 판별 식별자: isRetake
        String timtSmryCn,
        String facDvnm,
        String cltTerrNm,
        String cltTerrCd,
        String subjtEstbSmrCd,
        String subjtEstbYearSmr,
        String diclNo,
        String gainPont,
        Optional<String> cretDelCd, // 재수강 삭제 코드
        Optional<String> cretDelNm // 재수강 삭제 과목의 경우 '재수강 삭제' 문자열로 넘어옴
) {}