package com.chukchuk.haksa.application.dto;

import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 포털 연동·재연동 완료 후 작업 식별자와 학생 정보 및 처리 상태를 반환한다.
 *
 * @param taskId 응답 추적에 사용하는 작업 식별자
 * @param studentInfo 포털 연동으로 확인한 학생 정보
 * @param status 포털 연동 및 동기화 처리 상태
 */
@Schema(description = "포털 연동 또는 재연동 및 학업 이력 동기화 성공 응답")
public record ScrapingResponse(
    @Schema(description = "작업 ID", example = "4aabf0d0-1c23-4f3d-845e-24c9c943deed") String taskId,
    @Schema(description = "학생 정보 요약") PortalConnectionResult.StudentInfo studentInfo,
    @Schema(description = "포털 연동 상태", example = "SUCCESS / ALREADY_CONNECTED") String status) {
  /**
   * 성공한 스크래핑 작업의 응답을 생성한다.
   *
   * @param taskId 스크래핑 작업 식별자
   * @param studentInfo 포털에서 조회한 학생 정보
   * @return 성공 상태의 스크래핑 응답
   */
  public static ScrapingResponse success(
      String taskId, PortalConnectionResult.StudentInfo studentInfo) {
    return new ScrapingResponse(taskId, studentInfo, "SUCCESS");
  }
}
