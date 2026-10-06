package com.chukchuk.haksa.domain.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** 포털 로그인 검증과 연동 작업 API에서 사용하는 요청·응답 형식을 묶는다. */
public class PortalLinkDto {

  /**
   * 포털 연동 작업 생성에 필요한 자격 증명을 전달한다.
   *
   * @param portalType 포털 유형
   * @param username 포털 로그인 아이디
   * @param password 포털 비밀번호
   */
  @Schema(description = "포털 연동 job 생성 요청")
  public record LinkRequest(
      @NotBlank @JsonProperty("portal_type") @Schema(description = "포털 타입", example = "suwon")
          String portalType,
      @NotBlank @Schema(description = "포털 아이디", example = "17019013") String username,
      @NotBlank @Schema(description = "포털 비밀번호", example = "pw") String password) {}

  /**
   * 접수된 포털 연동 작업의 식별자와 상태 조회 경로를 전달한다.
   *
   * @param jobId 작업 식별자
   * @param status 상태
   * @param pollingEndpoint 작업 상태를 조회할 API 경로
   */
  @Schema(description = "스크래핑 job 수락 응답")
  public record AcceptedResponse(
      @JsonProperty("job_id") @Schema(description = "job id", example = "job-123") String jobId,
      @Schema(description = "수락 상태", example = "accepted") String status,
      @JsonProperty("polling_endpoint")
          @Schema(description = "상태 조회 경로", example = "/portal/link/jobs/job-123")
          String pollingEndpoint) {}

  /**
   * 포털 연동 작업의 현재 상태·오류·생성 및 완료 시각을 담는다.
   *
   * @param jobId 작업 식별자
   * @param portalType 포털 유형
   * @param status 상태
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param createdAt 작업 생성 시각
   * @param updatedAt 수정 시각
   * @param finishedAt 처리 종료 시각
   */
  @Schema(description = "스크래핑 job 상태 응답")
  public record JobStatusResponse(
      @JsonProperty("job_id") String jobId,
      @JsonProperty("portal_type") String portalType,
      String status,
      @JsonProperty("error_code") String errorCode,
      @JsonProperty("error_message") String errorMessage,
      Boolean retryable,
      @JsonProperty("created_at") Instant createdAt,
      @JsonProperty("updated_at") Instant updatedAt,
      @JsonProperty("finished_at") Instant finishedAt) {}

  /**
   * 완료된 포털 연동 작업의 학생 요약과 완료 시각을 담는다.
   *
   * @param jobId 작업 식별자
   * @param status 상태
   * @param studentInfo 연동으로 확인된 학생 요약 정보
   * @param finishedAt 처리 종료 시각
   */
  @Schema(description = "스크래핑 job 요약 응답")
  public record JobSummaryResponse(
      @JsonProperty("job_id") String jobId,
      String status,
      @JsonProperty("studentInfo") StudentInfoSummary studentInfo,
      @JsonProperty("finished_at") Instant finishedAt) {}

  /**
   * 포털 연동 작업의 시작·종료 시각과 계산된 소요 시간을 담는다.
   *
   * @param jobId 작업 식별자
   * @param status 상태
   * @param success 작업 성공 여부
   * @param startedAt 작업 시작 시각
   * @param endedAt 작업 종료 시각
   * @param elapsedMillis 밀리초 단위 처리 시간
   * @param elapsedTime 사람이 읽을 수 있는 처리 시간 문자열
   */
  @Schema(description = "스크래핑 job 소요 시간 응답")
  public record JobDurationResponse(
      @JsonProperty("job_id") String jobId,
      String status,
      Boolean success,
      @JsonProperty("started_at") Instant startedAt,
      @JsonProperty("ended_at") Instant endedAt,
      @JsonProperty("elapsed_millis") Long elapsedMillis,
      @JsonProperty("elapsed_time") String elapsedTime) {}

  /**
   * 포털 연동 완료 후 반환할 학생 요약을 표현한다.
   *
   * @param name 이름
   * @param school 학생이 소속된 학교명
   * @param majorName 전공 이름
   * @param studentCode 학번
   * @param gradeLevel 학년
   * @param status 상태
   * @param completedSemesterType 마지막으로 이수한 정규·계절 학기 코드
   */
  @Schema(description = "포털 학생 요약 정보")
  public record StudentInfoSummary(
      String name,
      String school,
      String majorName,
      String studentCode,
      int gradeLevel,
      String status,
      int completedSemesterType) {}

  /**
   * 스크래핑 워커가 전송한 결과 위치·처리 상태·재시도 정보와 메타데이터를 담는다.
   *
   * @param jobId 작업 식별자
   * @param status 상태
   * @param attempt 워커의 현재 처리 시도 횟수
   * @param resultS3Key 스크래핑 결과가 저장된 S3 객체 키
   * @param resultChecksum 결과 payload의 무결성 검증값
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param finishedAt 처리 종료 시각
   * @param metadata 워커가 함께 전달한 부가 처리 정보
   */
  public record ScrapeResultCallbackRequest(
      @JsonProperty("job_id") String jobId,
      String status,
      Integer attempt,
      @JsonProperty("result_s3_key") String resultS3Key,
      @JsonProperty("result_checksum") String resultChecksum,
      @JsonProperty("error_code") String errorCode,
      @JsonProperty("error_message") String errorMessage,
      Boolean retryable,
      @JsonProperty("finished_at") Instant finishedAt,
      @JsonProperty("metadata") JsonNode metadata) {}
}
