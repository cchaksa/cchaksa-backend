// 일반·편입생 졸업진단 응답의 JSON 계약을 검증한다.

package com.chukchuk.haksa.domain.graduation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraduationProgressResponseJsonTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void existingConstructorSerializesRegularAnalysisWithoutTransferProgress() {
    GraduationProgressResponse regular = new GraduationProgressResponse(List.of(), true);

    JsonNode json = objectMapper.valueToTree(regular);

    assertThat(json.path("analysisType").asText()).isEqualTo("REGULAR");
    assertThat(json.path("analysisStatus").asText()).isEqualTo("CALCULATED");
    assertThat(json.has("transferProgress")).isFalse();
  }

  @Test
  void transferFactorySerializesTransferAnalysisAndProgress() {
    GraduationProgressResponse transfer =
        GraduationProgressResponse.forTransfer(transferProgress(), null);

    JsonNode json = objectMapper.valueToTree(transfer);

    assertThat(json.path("analysisType").asText()).isEqualTo("TRANSFER");
    assertThat(json.path("analysisStatus").asText()).isEqualTo("MANUAL_REVIEW_REQUIRED");
    assertThat(json.path("graduationProgress")).isEmpty();
    assertThat(json.path("transferProgress").path("requiredTotalCredits").asInt()).isEqualTo(130);
    assertThat(json.path("languageCertFulfilled").isNull()).isTrue();
  }

  @Test
  void transferProgressSerializesAreaEvaluationStateAndNullableTarget() {
    TransferAreaProgressDto earnedOnlyArea =
        new TransferAreaProgressDto(
            com.chukchuk.haksa.domain.course.model.FacultyDivision.전취,
            TransferAreaEvaluationType.EARNED_ONLY,
            6,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of());
    TransferGraduationProgressDto transferProgress =
        new TransferGraduationProgressDto(
            130,
            112,
            18,
            false,
            65,
            new BigDecimal("3.2"),
            new BigDecimal("2.0"),
            true,
            3,
            false,
            List.of(),
            true,
            List.of(TransferManualReviewReason.REQUIRED_COURSES_NOT_ASSESSABLE),
            List.of(earnedOnlyArea),
            0,
            List.of());

    JsonNode json =
        objectMapper.valueToTree(GraduationProgressResponse.forTransfer(transferProgress, null));

    JsonNode area = json.path("transferProgress").path("areas").get(0);
    assertThat(area.path("evaluationType").asText()).isEqualTo("EARNED_ONLY");
    assertThat(area.path("earnedCredits").asInt()).isEqualTo(6);
    assertThat(area.path("requiredCredits").isNull()).isTrue();
    assertThat(area.path("fulfilled").isNull()).isTrue();
  }

  @Test
  void transferFactoryKeepsPartialDiagnosisWithoutManualReviewReasons() {
    TransferGraduationProgressDto progress =
        new TransferGraduationProgressDto(
            130,
            130,
            0,
            true,
            65,
            new BigDecimal("3.2"),
            new BigDecimal("2.0"),
            true,
            4,
            false,
            List.of(),
            false,
            List.of());

    JsonNode json =
        objectMapper.valueToTree(GraduationProgressResponse.forTransfer(progress, true));

    assertThat(json.path("analysisStatus").asText()).isEqualTo("MANUAL_REVIEW_REQUIRED");
    assertThat(json.path("transferProgress").path("manualReviewRequired").asBoolean()).isFalse();
  }

  @Test
  void transferFactoryPreservesExplicitAnalysisStatus() {
    JsonNode json =
        objectMapper.valueToTree(
            GraduationProgressResponse.forTransfer(
                transferProgress(), true, GraduationAnalysisStatus.CALCULATED));

    assertThat(json.path("analysisStatus").asText()).isEqualTo("CALCULATED");
  }

  private TransferGraduationProgressDto transferProgress() {
    return new TransferGraduationProgressDto(
        130,
        112,
        18,
        false,
        65,
        new BigDecimal("3.2"),
        new BigDecimal("2.0"),
        true,
        3,
        false,
        List.of(
            new DesignatedCourseProgressDto(
                "C101", "자료구조", 3, DesignatedCourseCompletionStatus.COMPLETED)),
        true,
        List.of(TransferManualReviewReason.REQUIRED_COURSES_NOT_ASSESSABLE));
  }
}
