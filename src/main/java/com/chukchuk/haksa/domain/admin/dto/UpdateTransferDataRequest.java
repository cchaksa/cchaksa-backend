// 테스트 계정의 편입 원천 데이터 부분 수정과 지정과목 입력을 검증한다.

package com.chukchuk.haksa.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;

/** 생략한 필드는 유지하고 명시적 null은 거부하는 편입 데이터 수정 요청이다. */
@Getter
@Schema(description = "편입 테스트 데이터 부분 수정. 생략은 유지, 명시적 null은 400입니다.")
public class UpdateTransferDataRequest {

  @JsonSetter(nulls = Nulls.FAIL)
  @Schema(description = "편입생 여부", example = "true")
  private Boolean isTransferStudent;

  @JsonSetter(nulls = Nulls.FAIL)
  @Min(0)
  @Schema(description = "누적 취득학점", example = "112")
  private Integer totalEarnedCredits;

  @JsonSetter(nulls = Nulls.FAIL)
  @DecimalMin("0.0")
  @DecimalMax("4.5")
  @Digits(integer = 1, fraction = 2)
  @Schema(description = "누적 GPA. 0~4.5, 소수 둘째 자리까지 허용합니다.", example = "3.2")
  private BigDecimal cumulativeGpa;

  @JsonSetter(nulls = Nulls.FAIL)
  @Min(0)
  @Schema(description = "이수 학기 수", example = "3")
  private Integer completedSemesters;

  @JsonSetter(nulls = Nulls.FAIL)
  @Schema(description = "외국어 인증 통과 여부", example = "true")
  private Boolean languageCertFulfilled;

  @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
  @Valid
  @Schema(description = "지정과목 전체 교체 목록. 빈 배열도 정상 수신 상태로 저장합니다.")
  private List<@NotNull @Valid DesignatedCourseRequest> designatedCourses;

  /**
   * 학생과 원본 순서는 서버가 부여하는 지정과목 입력이다.
   *
   * @param orgClsCd 지정과목 조직 분류 코드
   * @param subjtCd 실제 이수 내역과 비교할 과목 코드
   * @param subjtNm 원본 과목명
   * @param point 원본 지정학점이며 실제 취득학점과 구분한다
   * @param precpResnCd 지정 사유 코드
   * @param cretGainYear 원본 취득 연도
   * @param cretSmrNm 원본 취득 학기명
   */
  @Schema(description = "학생 학번과 순서는 인증 계정과 배열 순서로 결정하는 지정과목")
  public record DesignatedCourseRequest(
      @NotBlank @Size(max = 255) @Schema(example = "20") String orgClsCd,
      @NotBlank @Size(max = 255) @Schema(example = "C101") String subjtCd,
      @Size(max = 255) @Schema(example = "자료구조") String subjtNm,
      @Min(0) @Schema(description = "원본 지정학점", example = "3") Integer point,
      @Size(max = 255) String precpResnCd,
      @Min(1) @Max(9999) Integer cretGainYear,
      @Size(max = 255) String cretSmrNm) {}
}
