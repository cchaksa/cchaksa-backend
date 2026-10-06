// 포털 raw 데이터의 외국어 인증 값을 내부 모델로 변환하는 테스트

package com.chukchuk.haksa.infrastructure.portal.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
    RawPortalData raw =
        objectMapper.readValue(payloadWithNullPointAndGainPoint(), RawPortalData.class);

    PortalData portalData = PortalDataMapper.toPortalData(raw);

    assertThat(portalData.academic().semesters().get(0).courses().get(0).credits()).isEqualTo(4);
    assertThat(portalData.curriculum().offerings().get(0).points()).isEqualTo(4);
  }

  @Test
  @DisplayName("실제 편입생 포털 fixture의 학점·외국어·지정과목을 내부 모델로 변환한다")
  void mapsTransferGraduationFixture() throws Exception {
    RawPortalData raw = objectMapper.readValue(transferFixture(), RawPortalData.class);

    PortalData portalData = PortalDataMapper.toPortalData(raw);

    assertThat(portalData.student().admission().type()).isEqualTo("2");
    assertThat(portalData.student().languageCertFulfilled()).isTrue();
    assertThat(portalData.academic().summary().totalCredits()).isEqualTo(112);
    assertThat(portalData.academic().semesters().get(0).courses())
        .extracting(course -> course.code())
        .containsExactly("07045", "C101");
    assertThat(portalData.academic().semesters().get(0).courses().get(0).credits()).isEqualTo(65);
    assertThat(portalData.designatedCourses().courses())
        .extracting(DesignatedCourseData::subjtCd)
        .containsExactly("C101", "C202");
  }

  @Test
  @DisplayName("designatedCourses 배열은 순서와 중복을 보존하고 숫자 필드를 변환한다")
  void mapsDesignatedCoursesFromFixture() throws Exception {
    RawPortalData raw = objectMapper.readValue(fixture(), RawPortalData.class);

    PortalData mapped = PortalDataMapper.toPortalData(raw);

    assertThat(mapped.designatedCourses().received()).isTrue();
    assertThat(mapped.designatedCourses().courses())
        .extracting(DesignatedCourseData::sourceOrder)
        .containsExactly(0, 1);
    assertThat(mapped.designatedCourses().courses())
        .extracting(DesignatedCourseData::subjtCd)
        .containsExactly("C101", "C101");
    assertThat(mapped.designatedCourses().courses().get(0).point()).isEqualTo(3);
    assertThat(mapped.designatedCourses().courses().get(1).point()).isNull();
    assertThat(mapped.designatedCourses().courses().get(0).cretGainYear()).isEqualTo(2024);
    assertThat(mapped.designatedCourses().courses().get(1).cretGainYear()).isNull();
  }

  @Test
  @DisplayName("designatedCourses 필드가 누락되면 수신하지 않은 스냅샷으로 변환한다")
  void treatsMissingDesignatedCoursesAsNotReceived() throws Exception {
    RawPortalData raw =
        objectMapper.readValue(fixtureWithoutDesignatedCourses(), RawPortalData.class);

    PortalData mapped = PortalDataMapper.toPortalData(raw);

    assertThat(mapped.designatedCourses().received()).isFalse();
    assertThat(mapped.designatedCourses().courses()).isEmpty();
  }

  @Test
  @DisplayName("designatedCourses가 null이면 수신하지 않은 스냅샷으로 변환한다")
  void treatsNullDesignatedCoursesAsNotReceived() throws Exception {
    RawPortalData raw =
        objectMapper.readValue(fixtureWithDesignatedCourses("null"), RawPortalData.class);

    PortalData mapped = PortalDataMapper.toPortalData(raw);

    assertThat(mapped.designatedCourses().received()).isFalse();
    assertThat(mapped.designatedCourses().courses()).isEmpty();
  }

  @Test
  @DisplayName("designatedCourses가 빈 배열이면 수신한 빈 스냅샷으로 변환한다")
  void treatsEmptyDesignatedCoursesAsReceivedEmptySnapshot() throws Exception {
    RawPortalData raw =
        objectMapper.readValue(fixtureWithDesignatedCourses("[]"), RawPortalData.class);

    PortalData mapped = PortalDataMapper.toPortalData(raw);

    assertThat(mapped.designatedCourses().received()).isTrue();
    assertThat(mapped.designatedCourses().courses()).isEmpty();
  }

  @Test
  @DisplayName("지정과목 숫자 필드가 숫자가 아니면 입력을 거부한다")
  void rejectsInvalidDesignatedCourseNumber() throws Exception {
    RawPortalData raw =
        objectMapper.readValue(
            fixture().replace("\"point\": 3", "\"point\": \"3학점\""), RawPortalData.class);

    assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("point");
  }

  @Test
  @DisplayName("지정과목 학생번호가 최상위 학생번호와 다르면 입력을 거부한다")
  void rejectsDesignatedCourseStudentMismatch() throws Exception {
    RawPortalData raw =
        objectMapper.readValue(fixtureWithMismatchedDesignatedCourseStudent(), RawPortalData.class);

    assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("학생번호");
  }

  @Test
  @DisplayName("designatedCourses 배열에 null 행이 있으면 입력을 거부한다")
  void rejectsNullDesignatedCourseRow() throws Exception {
    ObjectNode root = (ObjectNode) newObjectMapper().readTree(fixture());
    ((ArrayNode) root.get("designatedCourses")).insertNull(0);
    RawPortalData raw = objectMapper.readValue(root.toString(), RawPortalData.class);

    assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("행");
  }

  @ParameterizedTest
  @CsvSource({"orgClsCd, null", "orgClsCd, '   '", "subjtCd, null", "subjtCd, '   '"})
  @DisplayName("지정과목의 필수 코드가 없으면 입력을 거부한다")
  void rejectsMissingDesignatedCourseCode(String field, String value) throws Exception {
    ObjectNode root = (ObjectNode) newObjectMapper().readTree(fixture());
    ObjectNode course = (ObjectNode) ((ArrayNode) root.get("designatedCourses")).get(0);
    if ("null".equals(value)) {
      course.putNull(field);
    } else {
      course.put(field, value.replace("'", ""));
    }
    RawPortalData raw = objectMapper.readValue(root.toString(), RawPortalData.class);

    assertThatThrownBy(() -> PortalDataMapper.toPortalData(raw))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(field);
  }

  @ParameterizedTest
  @CsvSource({"8영역, 8", "10영역, 10"})
  @DisplayName("숫자 영역명은 전체 영역 번호로 변환한다")
  void mapsFullAreaNumber(String rawAreaName, int expected) throws Exception {
    PortalData portalData = PortalDataMapper.toPortalData(areaPayload(rawAreaName));

    assertThat(portalData.curriculum().offerings().get(0).areaCode()).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"영역8", "8영역추가", "미지정"})
  @DisplayName("누락되거나 숫자+영역 형식이 아니면 영역 코드 없음으로 변환한다")
  void rejectsInvalidAreaFormat(String rawAreaName) throws Exception {
    PortalData portalData = PortalDataMapper.toPortalData(areaPayload(rawAreaName));

    assertThat(portalData.curriculum().offerings().get(0).areaCode()).isZero();
  }

  private static String payloadWithLanguageCert(String flangPassGb) {
    return String.format(
        """
        {
          "studentInfo":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8,"flangPassGb":"%s"},
          "semesters":[],
          "academicRecords":{
            "listSmrCretSumTabYearSmr":[],
            "selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}
          }
        }
        """,
        flangPassGb);
  }

  private static String payloadWithNullPointAndGainPoint() {
    return payloadWithLanguageCert("통과")
        .replace(
            "\"semesters\":[]",
            "\"semesters\":[{\"semester\":\"2025-10\",\"courses\":[{\"subjtCd\":\"C101\","
                + "\"subjtNm\":\"학점보정\",\"point\":null,\"gainPoint\":4,"
                + "\"cretDelNm\":null}]}]");
  }

  private static String fixture() throws Exception {
    try (InputStream input =
        PortalDataMapperTests.class.getResourceAsStream(
            "/fixtures/portal/designated-courses.json")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String transferFixture() throws Exception {
    try (InputStream input =
        PortalDataMapperTests.class.getResourceAsStream(
            "/fixtures/portal/transfer-graduation.json")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String fixtureWithoutDesignatedCourses() throws Exception {
    ObjectNode root = (ObjectNode) newObjectMapper().readTree(fixture());
    root.remove("designatedCourses");
    return newObjectMapper().writeValueAsString(root);
  }

  private static String fixtureWithDesignatedCourses(String value) throws Exception {
    ObjectNode root = (ObjectNode) newObjectMapper().readTree(fixture());
    if ("null".equals(value)) {
      root.putNull("designatedCourses");
    } else {
      root.set("designatedCourses", newObjectMapper().createArrayNode());
    }
    return newObjectMapper().writeValueAsString(root);
  }

  private static String fixtureWithMismatchedDesignatedCourseStudent() throws Exception {
    ObjectNode root = (ObjectNode) newObjectMapper().readTree(fixture());
    ArrayNode courses = (ArrayNode) root.get("designatedCourses");
    ((ObjectNode) courses.get(0)).put("sno", "99999999");
    return newObjectMapper().writeValueAsString(root);
  }

  private static ObjectMapper newObjectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }

  private RawPortalData areaPayload(String areaName) throws Exception {
    String rawAreaName = areaName == null ? "null" : "\"%s\"".formatted(areaName);
    String payload =
        payloadWithLanguageCert("통과")
            .replace(
                "\"semesters\":[]",
                "\"semesters\":[{\"semester\":\"2025-10\",\"courses\":[{"
                    + "\"subjtCd\":\"C101\",\"cltTerrNm\":%s,\"cretDelNm\":null}]}]"
                        .formatted(rawAreaName));
    return objectMapper.readValue(payload, RawPortalData.class);
  }
}
