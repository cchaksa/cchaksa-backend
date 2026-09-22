// 사용자 문의 API의 요청과 응답 계약을 정의한다.

package com.chukchuk.haksa.domain.report.dto;

import com.chukchuk.haksa.domain.report.model.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 사용자 문의 생성·목록·상세 API에서 사용하는 데이터 계약이다. */
public final class ReportDto {

  private ReportDto() {}

  /** 문의 생성 요청이다. */
  public record CreateRequest(
      @Schema(description = "문의 제목", example = "졸업 요건 확인이 불가능합니다.") @NotBlank @Size(max = 100)
          String title,
      @Schema(description = "문의 본문", example = "졸업 요건 화면을 확인할 수 없습니다.") @NotBlank @Size(max = 5000)
          String content) {

    /**
     * 입력 양끝의 공백을 제거한다.
     *
     * @param title 문의 제목
     * @param content 문의 본문
     */
    public CreateRequest {
      title = title == null ? null : title.trim();
      content = content == null ? null : content.trim();
    }
  }

  /** 문의 생성 결과다. */
  public record CreateResponse(
      @Schema(description = "문의 식별자") UUID id,
      @Schema(description = "답변 상태") ReportStatus status,
      @Schema(description = "생성 시각") Instant createdAt) {}

  /** 문의 목록의 한 항목이다. */
  public record ListItem(
      @Schema(description = "문의 식별자") UUID id,
      @Schema(description = "답변 상태") ReportStatus status,
      @Schema(description = "문의 제목") String title,
      @Schema(description = "생성 시각") Instant createdAt,
      @Schema(description = "답변 완료 시각", nullable = true) Instant answeredAt) {}

  /** 문의 목록 페이지다. */
  public record PageResponse(
      @Schema(description = "문의 목록") List<ListItem> items,
      @Schema(description = "현재 페이지", minimum = "0") @Min(0) int page,
      @Schema(description = "페이지 크기", minimum = "1", maximum = "100") @Min(1) @Max(100) int size,
      @Schema(description = "전체 문의 수") long totalElements,
      @Schema(description = "전체 페이지 수") int totalPages,
      @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {}

  /** 문의 상세 응답이다. */
  public record DetailResponse(
      @Schema(description = "문의 식별자") UUID id,
      @Schema(description = "답변 상태") ReportStatus status,
      @Schema(description = "문의 제목") String title,
      @Schema(description = "문의 본문") String content,
      @Schema(description = "생성 시각") Instant createdAt,
      @Schema(description = "관리자 답변", nullable = true) String answer,
      @Schema(description = "답변 완료 시각", nullable = true) Instant answeredAt) {}
}
