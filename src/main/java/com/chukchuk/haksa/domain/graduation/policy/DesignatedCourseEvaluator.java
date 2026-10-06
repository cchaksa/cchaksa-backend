// 지정과목 이수 여부와 편입 인정학점을 수강 기록에서 계산한다.

package com.chukchuk.haksa.domain.graduation.policy;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseCompletionStatus;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseProgressDto;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 지정과목과 실제 수강 기록을 비교하는 저장소 비의존 평가기다. */
@Component
public class DesignatedCourseEvaluator {

  private static final Set<String> TRANSFER_CREDIT_CODES =
      Set.of("07045", "07046", "00111", "07050");
  private static final Set<GradeType> NON_PASSING_GRADES =
      EnumSet.of(GradeType.F, GradeType.R, GradeType.NP, GradeType.IP);

  /**
   * 지정과목 이수 상태와 편입 인정학점을 함께 계산한다.
   *
   * @param designatedCourses 학생에게 저장된 지정과목 원본 목록
   * @param studentCourses 학생의 전체 수강 기록
   * @return 지정과목 상태 목록과 편입 인정학점
   */
  public Evaluation evaluate(
      List<StudentDesignatedCourse> designatedCourses, List<StudentCourse> studentCourses) {
    Map<String, Integer> completedCourseCredits = new HashMap<>();
    Set<String> completedCourseCodes = new HashSet<>();
    boolean recognizedCreditUnknown = false;
    for (StudentCourse studentCourse :
        studentCourses == null ? Collections.<StudentCourse>emptyList() : studentCourses) {
      if (!isValidCompletedCourse(studentCourse)) {
        continue;
      }

      String courseCode = normalizeCode(studentCourse.getOffering().getCourse().getCourseCode());
      if (courseCode == null) {
        continue;
      }
      completedCourseCodes.add(courseCode);

      Integer credits = studentCourse.getPoints();
      if (credits == null) {
        if (TRANSFER_CREDIT_CODES.contains(courseCode)) {
          recognizedCreditUnknown = true;
        }
        continue;
      }
      completedCourseCredits.merge(courseCode, credits, Math::max);
    }

    List<DesignatedCourseProgressDto> progress =
        (designatedCourses == null
                ? Collections.<StudentDesignatedCourse>emptyList()
                : designatedCourses)
            .stream().map(course -> toProgress(course, completedCourseCodes)).toList();

    Integer recognizedTransferCredits =
        recognizedCreditUnknown
            ? null
            : TRANSFER_CREDIT_CODES.stream()
                .mapToInt(code -> completedCourseCredits.getOrDefault(code, 0))
                .sum();
    return new Evaluation(progress, recognizedTransferCredits);
  }

  /**
   * 정규화한 편입생 수강 기록으로 지정과목 상태를 계산한다.
   *
   * @param designatedCourses 학생에게 저장된 지정과목 원본 목록
   * @param courseEvaluation 편입생 수강 기록 평가 결과
   * @return 지정과목 상태와 편입 인정학점
   */
  public Evaluation evaluate(
      List<StudentDesignatedCourse> designatedCourses,
      TransferCourseEvaluator.Evaluation courseEvaluation) {
    Set<String> completedCourseCodes =
        courseEvaluation.courses().stream()
            .map(CourseInternalDto::getCourseCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    List<DesignatedCourseProgressDto> progress =
        (designatedCourses == null
                ? Collections.<StudentDesignatedCourse>emptyList()
                : designatedCourses)
            .stream().map(course -> toProgress(course, completedCourseCodes)).toList();
    return new Evaluation(progress, courseEvaluation.recognizedTransferCredits());
  }

  private DesignatedCourseProgressDto toProgress(
      StudentDesignatedCourse designatedCourse, Set<String> completedCourseCodes) {
    String normalizedCode = normalizeCode(designatedCourse.getSubjtCd());
    DesignatedCourseCompletionStatus status;
    if (normalizedCode == null) {
      status = DesignatedCourseCompletionStatus.UNKNOWN;
    } else if (completedCourseCodes.contains(normalizedCode)) {
      status = DesignatedCourseCompletionStatus.COMPLETED;
    } else {
      status = DesignatedCourseCompletionStatus.NOT_COMPLETED;
    }

    return new DesignatedCourseProgressDto(
        normalizedCode, designatedCourse.getSubjtNm(), designatedCourse.getPoint(), status);
  }

  private boolean isValidCompletedCourse(StudentCourse studentCourse) {
    return studentCourse != null
        && studentCourse.getOffering() != null
        && studentCourse.getOffering().getCourse() != null
        && studentCourse.getGrade() != null
        && studentCourse.getGrade().getValue() != null
        && !NON_PASSING_GRADES.contains(studentCourse.getGrade().getValue())
        && !studentCourse.isRetakeDeleted();
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    return code.trim().toUpperCase(Locale.ROOT);
  }

  /**
   * 지정과목 평가 결과를 보관한다.
   *
   * @param designatedCourses 지정과목별 이수 상태
   * @param recognizedTransferCredits 편입 인정학점 합계
   */
  public record Evaluation(
      List<DesignatedCourseProgressDto> designatedCourses, Integer recognizedTransferCredits) {
    /**
     * 지정과목 평가 목록을 방어적 복사해 평가 결과를 생성한다.
     *
     * @param designatedCourses 지정과목별 이수 상태
     * @param recognizedTransferCredits 편입 인정학점 합계
     */
    public Evaluation {
      designatedCourses = List.copyOf(designatedCourses);
    }
  }
}
