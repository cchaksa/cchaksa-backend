package com.chukchuk.haksa.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class ReportRepositoryIntegrationTest {

  @Autowired private ReportRepository reportRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("사용자별 문의만 생성 시각 역순으로 페이지 조회한다")
  void findsOnlyOwnerReportsInNewestOrder() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    Report older = save(ownerId, "오래된 문의");
    Report newer = save(ownerId, "최신 문의");
    save(otherId, "다른 사용자 문의");
    jdbcTemplate.update(
        "UPDATE reports SET created_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-09-21T00:00:00Z")),
        older.getId());
    jdbcTemplate.update(
        "UPDATE reports SET created_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-09-22T00:00:00Z")),
        newer.getId());
    entityManager.clear();

    Page<Report> page =
        reportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(ownerId, PageRequest.of(0, 1));

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.getContent()).extracting(Report::getTitle).containsExactly("최신 문의");
  }

  @Test
  @DisplayName("생성 시각이 같으면 문의 식별자 역순으로 조회한다")
  void usesIdDescendingAsCreatedAtTieBreaker() {
    UUID ownerId = UUID.randomUUID();
    UUID lowerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID higherId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    Timestamp sameCreatedAt = Timestamp.from(Instant.parse("2026-09-22T00:00:00Z"));
    insertReport(lowerId, ownerId, "낮은 식별자", sameCreatedAt);
    insertReport(higherId, ownerId, "높은 식별자", sameCreatedAt);
    entityManager.clear();

    Page<Report> page =
        reportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(ownerId, PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(Report::getId).containsExactly(higherId, lowerId);
  }

  private Report save(UUID userId, String title) {
    return reportRepository.saveAndFlush(
        Report.create(userId, title, "문의 본문", ReportSubmitterSnapshot.unknown(userId)));
  }

  private void insertReport(UUID reportId, UUID userId, String title, Timestamp createdAt) {
    jdbcTemplate.update(
        """
        INSERT INTO reports (
            id, user_id, submitted_user_id, title, content, status,
            graduation_requirement_status, created_at, updated_at
        ) VALUES (?, ?, ?, ?, '문의 본문', 'PENDING', 'UNKNOWN', ?, ?)
        """,
        reportId,
        userId,
        userId,
        title,
        createdAt,
        createdAt);
  }
}
