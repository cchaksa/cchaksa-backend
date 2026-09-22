// 사용자 문의의 상태·답변·스냅샷 불변식을 검증한다.

package com.chukchuk.haksa.domain.report.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportTest {

  @Test
  void createsPendingReportWithNormalizedText() {
    UUID userId = UUID.randomUUID();
    ReportSubmitterSnapshot snapshot =
        new ReportSubmitterSnapshot(
            userId,
            1L,
            "컴퓨터학과",
            "20260001",
            1L,
            "컴퓨터학과",
            null,
            null,
            false,
            2026,
            GraduationRequirementSnapshotStatus.AVAILABLE);

    Report report = Report.create(userId, "  문의 제목  ", "  문의 본문  ", snapshot);

    assertThat(report.getUserId()).isEqualTo(userId);
    assertThat(report.getTitle()).isEqualTo("문의 제목");
    assertThat(report.getContent()).isEqualTo("문의 본문");
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(report.getAnswer()).isNull();
    assertThat(report.getAnsweredAt()).isNull();
  }

  @Test
  void completesAnswerAtomically() {
    UUID userId = UUID.randomUUID();
    Report report = Report.create(userId, "제목", "본문", ReportSubmitterSnapshot.unknown(userId));
    Instant answeredAt = Instant.parse("2026-09-22T01:00:00Z");

    report.answer("  답변입니다.  ", answeredAt);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.ANSWERED);
    assertThat(report.getAnswer()).isEqualTo("답변입니다.");
    assertThat(report.getAnsweredAt()).isEqualTo(answeredAt);
  }

  @Test
  void rejectsBlankAnswer() {
    Report report =
        Report.create(
            UUID.randomUUID(), "제목", "본문", ReportSubmitterSnapshot.unknown(UUID.randomUUID()));

    assertThatThrownBy(() -> report.answer("   ", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anonymizesStructuredSnapshotButKeepsConversation() {
    UUID userId = UUID.randomUUID();
    ReportSubmitterSnapshot snapshot =
        new ReportSubmitterSnapshot(
            userId,
            1L,
            "컴퓨터학과",
            "20260001",
            1L,
            "컴퓨터학과",
            2L,
            "경영학과",
            true,
            2026,
            GraduationRequirementSnapshotStatus.AVAILABLE);
    Report report = Report.create(userId, "제목", "본문", snapshot);
    report.answer("답변", Instant.parse("2026-09-22T01:00:00Z"));

    report.anonymizeSubmitterSnapshot();

    assertThat(report.getTitle()).isEqualTo("제목");
    assertThat(report.getContent()).isEqualTo("본문");
    assertThat(report.getAnswer()).isEqualTo("답변");
    assertThat(report.getSubmitterSnapshot().getSubmittedUserId()).isNull();
    assertThat(report.getSubmitterSnapshot().getStudentCode()).isNull();
    assertThat(report.getSubmitterSnapshot().getGraduationRequirementStatus())
        .isEqualTo(GraduationRequirementSnapshotStatus.UNKNOWN);
  }
}
