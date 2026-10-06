// 편입생 영역별 취득학점과 기준 비교 규칙을 검증한다.

package com.chukchuk.haksa.domain.graduation.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import com.chukchuk.haksa.domain.graduation.dto.TransferAreaEvaluationType;
import com.chukchuk.haksa.domain.graduation.dto.TransferAreaProgressDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TransferAreaEvaluatorTest {

  private final TransferAreaEvaluator evaluator = new TransferAreaEvaluator();

  @Test
  void keepsEarnedOnlyAreaWithoutInventingRequiredCredits() {
    TransferCourseEvaluator.Evaluation courses = evaluation(course("일선", "E101", 6));

    List<TransferAreaProgressDto> result =
        evaluator.evaluate(courses, TransferAreaEvaluator.Requirements.unavailable());

    TransferAreaProgressDto area =
        result.stream()
            .filter(value -> value.areaType() == FacultyDivision.일선)
            .findFirst()
            .orElseThrow();
    assertThat(area.evaluationType()).isEqualTo(TransferAreaEvaluationType.EARNED_ONLY);
    assertThat(area.earnedCredits()).isEqualTo(6);
    assertThat(area.requiredCredits()).isNull();
    assertThat(area.fulfilled()).isNull();
  }

  @ParameterizedTest
  @CsvSource({"8,9,false", "9,9,true", "10,9,true", "9,9.5,false", "10,9.5,true"})
  void comparesCoreCreditsWithoutRequiringIndividualCourses(
      int earned, String required, boolean fulfilled) {
    TransferCourseEvaluator.Evaluation courses = evaluation(course("전핵", "C999", earned));
    TransferAreaEvaluator.Requirements requirements =
        new TransferAreaEvaluator.Requirements(
            new BigDecimal(required), new BigDecimal("48"), List.of(), List.of());
    TransferAreaProgressDto core =
        evaluator.evaluate(courses, requirements).stream()
            .filter(value -> value.areaType() == FacultyDivision.전핵)
            .findFirst()
            .orElseThrow();
    assertThat(core.evaluationType()).isEqualTo(TransferAreaEvaluationType.COMPARISON);
    assertThat(core.earnedCredits()).isEqualTo(earned);
    assertThat(core.countedCredits()).isEqualTo(earned);
    assertThat(core.requiredCredits()).isEqualByComparingTo(required);
    assertThat(core.fulfilled()).isEqualTo(fulfilled);
    assertThat(core.requiredCourses()).isEmpty();
  }

  @Test
  void keepsFulfillmentUnknownWhenEarnedCreditsAreUnknown() {
    CourseInternalDto course =
        new CourseInternalDto(1L, "전핵", null, "P", "C101", 1, 2026, "C101", null, null);
    TransferAreaProgressDto core =
        evaluator
            .evaluate(
                evaluation(course),
                new TransferAreaEvaluator.Requirements(
                    new BigDecimal("9"), new BigDecimal("48"), List.of(), List.of()))
            .stream()
            .filter(value -> value.areaType() == FacultyDivision.전핵)
            .findFirst()
            .orElseThrow();
    assertThat(core.evaluationType()).isEqualTo(TransferAreaEvaluationType.COMPARISON);
    assertThat(core.earnedCredits()).isNull();
    assertThat(core.fulfilled()).isNull();
    assertThat(core.unavailableReasons()).containsExactly("COURSE_DATA_INCOMPLETE");
  }

  @Test
  void reportsUnavailableComparisonWithoutDroppingKnownEarnedCredits() {
    TransferCourseEvaluator.Evaluation courses = evaluation(course("전선", "E101", 45));
    TransferAreaEvaluator.Requirements requirements =
        new TransferAreaEvaluator.Requirements(
            null,
            null,
            List.of("CORE_CURRICULUM_UNAVAILABLE"),
            List.of("ELECTIVE_REQUIREMENT_UNAVAILABLE"));

    TransferAreaProgressDto elective =
        evaluator.evaluate(courses, requirements).stream()
            .filter(value -> value.areaType() == FacultyDivision.전선)
            .findFirst()
            .orElseThrow();

    assertThat(elective.evaluationType()).isEqualTo(TransferAreaEvaluationType.UNAVAILABLE);
    assertThat(elective.earnedCredits()).isEqualTo(45);
    assertThat(elective.requiredCredits()).isNull();
    assertThat(elective.fulfilled()).isNull();
    assertThat(elective.unavailableReasons()).containsExactly("ELECTIVE_REQUIREMENT_UNAVAILABLE");
  }

  private TransferCourseEvaluator.Evaluation evaluation(CourseInternalDto... courses) {
    Map<String, Integer> earned =
        java.util.Arrays.stream(courses)
            .filter(value -> value.getCredits() != null)
            .collect(
                java.util.stream.Collectors.toMap(
                    CourseInternalDto::getCourseCode, CourseInternalDto::getCredits));
    return new TransferCourseEvaluator.Evaluation(List.of(courses), earned, earned, Set.of(), 0);
  }

  private CourseInternalDto course(String area, String code, int credits) {
    return new CourseInternalDto(1L, area, credits, "A0", code, 1, 2025, code, 90, null);
  }
}
