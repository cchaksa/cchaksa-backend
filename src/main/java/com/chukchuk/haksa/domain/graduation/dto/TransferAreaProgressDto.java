// 편입생의 영역별 이수학점과 기준 비교 결과를 전달한다.

package com.chukchuk.haksa.domain.graduation.dto;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** 편입생 한 영역의 이수 현황과 평가 상태다. */
@Schema(description = "편입생 영역별 이수 현황")
public record TransferAreaProgressDto(
    @Schema(description = "영역 유형", example = "전선") FacultyDivision areaType,
    @Schema(description = "영역 평가 방식") TransferAreaEvaluationType evaluationType,
    @Schema(description = "영역 전체 취득학점", nullable = true) Integer earnedCredits,
    @Schema(description = "기준 비교에 포함되는 취득학점", nullable = true) Integer countedCredits,
    @Schema(description = "편입연도에서 2년 전 일반 학생 전핵·전선 기준학점의 50%. 소수점 기준을 유지한다.", nullable = true)
        BigDecimal requiredCredits,
    @Schema(description = "영역 기준 충족 여부", nullable = true) Boolean fulfilled,
    @Schema(description = "영역에 포함된 이수 과목") List<CourseDto> courses,
    @Schema(description = "기존 응답 호환용 필드. 전핵·전선은 학점으로 비교하므로 빈 목록을 반환한다.")
        List<DesignatedCourseProgressDto> requiredCourses,
    @Schema(description = "영역 평가가 불가능한 사유 코드") List<String> unavailableReasons) {

  /** 목록 필드를 방어적 복사해 응답을 생성한다. */
  public TransferAreaProgressDto {
    courses = List.copyOf(courses);
    requiredCourses = List.copyOf(requiredCourses);
    unavailableReasons = List.copyOf(unavailableReasons);
  }
}
