// 포털 raw 데이터의 외국어 인증 값을 내부 모델로 변환하는 테스트
package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortalDataMapperTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("flangPassGb가 Y이면 알 수 없는 값으로 거부한다")
    void rejectsLanguageCertWhenFlagIsY() throws Exception {
        RawPortalData raw = objectMapper.readValue(payloadWithLanguageCert("Y"), RawPortalData.class);

        assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알 수 없는 외국어 인증 값: Y");
    }

    @Test
    @DisplayName("flangPassGb가 N이면 알 수 없는 값으로 거부한다")
    void rejectsLanguageCertWhenFlagIsN() throws Exception {
        RawPortalData raw = objectMapper.readValue(payloadWithLanguageCert("N"), RawPortalData.class);

        assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알 수 없는 외국어 인증 값: N");
    }

    @Test
    @DisplayName("flangPassGb가 통과이면 외국어 인증 통과로 변환한다")
    void mapsLanguageCertFulfilledWhenFlagIsKoreanPass() throws Exception {
        RawPortalData raw = objectMapper.readValue(payloadWithLanguageCert("통과"), RawPortalData.class);

        PortalData portalData = PortalDataMapper.toPortalData(raw);

        assertThat(portalData.student().languageCertFulfilled()).isTrue();
    }

    @Test
    @DisplayName("flangPassGb가 미통과이면 외국어 인증 미통과로 변환한다")
    void mapsLanguageCertNotFulfilledWhenFlagIsKoreanFail() throws Exception {
        RawPortalData raw = objectMapper.readValue(payloadWithLanguageCert("미통과"), RawPortalData.class);

        PortalData portalData = PortalDataMapper.toPortalData(raw);

        assertThat(portalData.student().languageCertFulfilled()).isFalse();
    }

    @Test
    @DisplayName("point가 없으면 gainPoint를 과목 학점으로 변환한다")
    void mapsGainPointWhenCoursePointIsNull() throws Exception {
        RawPortalData raw = objectMapper.readValue(payloadWithNullPointAndGainPoint(), RawPortalData.class);

        PortalData portalData = PortalDataMapper.toPortalData(raw);

        assertThat(portalData.academic().semesters().get(0).courses().get(0).credits()).isEqualTo(4);
        assertThat(portalData.curriculum().offerings().get(0).points()).isEqualTo(4);
    }

    private static String payloadWithLanguageCert(String flangPassGb) {
        return """
                {
                  "studentInfo":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8,"flangPassGb":"%s"},
                  "semesters":[],
                  "academicRecords":{
                    "listSmrCretSumTabYearSmr":[],
                    "selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}
                  }
                }
                """.formatted(flangPassGb);
    }

    private static String payloadWithNullPointAndGainPoint() {
        return payloadWithLanguageCert("통과").replace(
                "\"semesters\":[]",
                "\"semesters\":[{\"semester\":\"2025-10\",\"courses\":[{\"subjtCd\":\"C101\",\"subjtNm\":\"학점보정\",\"point\":null,\"gainPoint\":4,\"cretDelNm\":null}]}]"
        );
    }
}
