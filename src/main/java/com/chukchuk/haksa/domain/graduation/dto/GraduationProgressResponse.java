package com.chukchuk.haksa.domain.graduation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

/** 학생의 영역별 졸업 요건 충족도와 전체 졸업 가능 여부를 담는다. */
@Getter
@Schema(description = "졸업 요건 진행 상황 응답")
public class GraduationProgressResponse {
  @Schema(description = "졸업진단 학생 유형", required = true)
  private GraduationAnalysisType analysisType;

  @Schema(description = "졸업진단 분석 상태", required = true)
  private GraduationAnalysisStatus analysisStatus;

  @Schema(description = "졸업 요건 영역별 이수 현황", required = true)
  private List<AreaProgressDto> graduationProgress;

  @Schema(description = "외국어 졸업 인증 통과 여부. 새 크롤러 동기화 전이면 null", nullable = true)
  private Boolean languageCertFulfilled;

  @Schema(description = "외국어 졸업 인증 정보를 확인하려면 포털 새로고침이 필요한지 여부", required = true)
  private boolean languageCertNeedsRefresh;

  @Schema(description = "특정 학과/연도 예외로 기존과 다른 졸업요건이 적용되는지 여부", required = true)
  private boolean hasDifferentGraduationRequirement = false;

  @Schema(description = "편입생 전용 졸업요건 진단 결과", nullable = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private TransferGraduationProgressDto transferProgress;

  /**
   * 외국어 인증 정보가 아직 없는 영역별 졸업 진행 응답을 생성한다.
   *
   * @param graduationProgress API 응답 형태로 변환할 졸업 진행률 계산 결과
   */
  public GraduationProgressResponse(List<AreaProgressDto> graduationProgress) {
    this(graduationProgress, null);
  }

  /**
   * 영역별 진행 상태와 외국어 인증 충족 여부로 졸업 진행 응답을 생성한다.
   *
   * @param graduationProgress API 응답 형태로 변환할 졸업 진행률 계산 결과
   * @param languageCertFulfilled 어학 인증 충족 여부
   */
  public GraduationProgressResponse(
      List<AreaProgressDto> graduationProgress, Boolean languageCertFulfilled) {
    this(
        GraduationAnalysisType.REGULAR,
        GraduationAnalysisStatus.CALCULATED,
        graduationProgress,
        languageCertFulfilled,
        null);
  }

  private GraduationProgressResponse(
      GraduationAnalysisType analysisType,
      GraduationAnalysisStatus analysisStatus,
      List<AreaProgressDto> graduationProgress,
      Boolean languageCertFulfilled,
      TransferGraduationProgressDto transferProgress) {
    this.analysisType = analysisType;
    this.analysisStatus = analysisStatus;
    this.graduationProgress = graduationProgress;
    this.languageCertFulfilled = languageCertFulfilled;
    this.languageCertNeedsRefresh = languageCertFulfilled == null;
    this.transferProgress = transferProgress;
  }

  /**
   * 편입생 부분 진단 결과를 API 응답으로 감싼다.
   *
   * @param transferProgress 편입생 부분 진단 결과
   * @param languageCertFulfilled 외국어 인증 충족 여부
   * @return 편입생 졸업진단 응답
   */
  public static GraduationProgressResponse forTransfer(
      TransferGraduationProgressDto transferProgress, Boolean languageCertFulfilled) {
    return forTransfer(
        transferProgress, languageCertFulfilled, GraduationAnalysisStatus.MANUAL_REVIEW_REQUIRED);
  }

  /**
   * 계산 결과에 맞는 상태를 지정해 편입생 진단 응답을 생성한다.
   *
   * @param transferProgress 편입생 진단 결과
   * @param languageCertFulfilled 외국어 인증 통과 여부
   * @param analysisStatus 분석 상태
   * @return 편입생 졸업진단 응답
   */
  public static GraduationProgressResponse forTransfer(
      TransferGraduationProgressDto transferProgress,
      Boolean languageCertFulfilled,
      GraduationAnalysisStatus analysisStatus) {
    return new GraduationProgressResponse(
        GraduationAnalysisType.TRANSFER,
        analysisStatus,
        List.of(),
        languageCertFulfilled,
        transferProgress);
  }

  /** 응답에 일반 기준과 다른 학과·연도별 졸업 요건이 적용됐음을 표시한다. */
  public void setHasDifferentGraduationRequirement() {
    this.hasDifferentGraduationRequirement = true;
  }
}
