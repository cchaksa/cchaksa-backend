// 사용자 문의와 답변 상태 및 생성 시점 제출자 정보를 저장한다.

package com.chukchuk.haksa.domain.report.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 제출한 문의와 관리자 답변 상태를 관리한다. */
@Entity
@Getter
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, unique = true)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReportStatus status;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @Column(name = "answered_at")
  private Instant answeredAt;

  @Embedded private ReportSubmitterSnapshot submitterSnapshot;

  private Report(
      UUID userId, String title, String content, ReportSubmitterSnapshot submitterSnapshot) {
    this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
    this.title = normalizeRequiredText(title, 100, "title");
    this.content = normalizeRequiredText(content, 5000, "content");
    this.submitterSnapshot = Objects.requireNonNull(submitterSnapshot, "submitterSnapshot은 필수입니다.");
    this.status = ReportStatus.PENDING;
  }

  /**
   * 인증 사용자와 생성 시점 스냅샷으로 답변 대기 문의를 생성한다.
   *
   * @param userId 현재 문의 소유자 식별자
   * @param title 문의 제목
   * @param content 문의 본문
   * @param submitterSnapshot 생성 당시 사용자·학적 정보
   * @return 답변 대기 상태의 문의
   */
  public static Report create(
      UUID userId, String title, String content, ReportSubmitterSnapshot submitterSnapshot) {
    return new Report(userId, title, content, submitterSnapshot);
  }

  /**
   * 관리자 답변과 완료 시각을 함께 기록한다.
   *
   * @param answer 저장할 답변
   * @param answeredAt 답변 완료 시각
   */
  public void answer(String answer, Instant answeredAt) {
    this.answer = normalizeRequiredText(answer, Integer.MAX_VALUE, "answer");
    this.answeredAt = Objects.requireNonNull(answeredAt, "answeredAt은 필수입니다.");
    this.status = ReportStatus.ANSWERED;
  }

  /** 탈퇴 사용자의 구조화된 제출자 스냅샷을 제거한다. */
  public void anonymizeSubmitterSnapshot() {
    submitterSnapshot.anonymize();
  }

  /**
   * 현재 인증 사용자가 문의 소유자인지 확인한다.
   *
   * @param candidateUserId 확인할 사용자 식별자
   * @return 현재 소유자이면 {@code true}
   */
  public boolean isOwnedBy(UUID candidateUserId) {
    return userId.equals(candidateUserId);
  }

  private static String normalizeRequiredText(String value, int maxLength, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "은 필수입니다.");
    }
    String normalized = value.trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) {
      throw new IllegalArgumentException(fieldName + " 길이가 유효하지 않습니다.");
    }
    return normalized;
  }
}
