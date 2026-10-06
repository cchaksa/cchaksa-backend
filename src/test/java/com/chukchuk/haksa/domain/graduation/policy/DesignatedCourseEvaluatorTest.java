// 지정과목 이수와 편입 인정학점 평가 규칙을 검증한다.

package com.chukchuk.haksa.domain.graduation.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseCompletionStatus;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseProgressDto;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DesignatedCourseEvaluatorTest {

  private final DesignatedCourseEvaluator evaluator = new DesignatedCourseEvaluator();

  @Test
  void keepsPassedDesignatedCourseCompletedWhenPersonalCreditsAreMissing() {
    List<StudentDesignatedCourse> designated = List.of(designatedCourse(" c101 ", "자료구조", 3, 0));
    List<StudentCourse> courses = List.of(studentCourse("C101", GradeType.P, null, false));

    assertThat(evaluator.evaluate(designated, courses).designatedCourses().get(0).status())
        .isEqualTo(DesignatedCourseCompletionStatus.COMPLETED);
  }

  @Test
  void keepsNormalizedPassedCourseCompletedWhenPersonalCreditsAreMissing() {
    List<StudentDesignatedCourse> designated = List.of(designatedCourse(" c101 ", "자료구조", 3, 0));
    TransferCourseEvaluator.Evaluation courses =
        new TransferCourseEvaluator()
            .evaluate(List.of(studentCourse("C101", GradeType.P, null, false)));

    assertThat(evaluator.evaluate(designated, courses).designatedCourses().get(0).status())
        .isEqualTo(DesignatedCourseCompletionStatus.COMPLETED);
  }

  @Test
  @DisplayName("과목 코드를 정규화하고 유효한 성적만 지정과목 이수로 인정한다")
  void evaluatesDesignatedCourseByNormalizedCodeAndPassingGrade() {
    StudentDesignatedCourse designated = designatedCourse(" abc123 ", "자료구조", 3, 0);

    DesignatedCourseEvaluator.Evaluation result =
        evaluator.evaluate(List.of(designated), List.of(studentCourse("ABC123", GradeType.P)));

    assertThat(result.designatedCourses())
        .extracting(DesignatedCourseProgressDto::status)
        .containsExactly(DesignatedCourseCompletionStatus.COMPLETED);
  }

  @Test
  @DisplayName("F, R, NP, IP와 재수강 삭제 과목은 지정과목 이수로 인정하지 않는다")
  void excludesNonPassingAndRetakeDeletedCourses() {
    for (GradeType gradeType : List.of(GradeType.F, GradeType.R, GradeType.NP, GradeType.IP)) {
      StudentDesignatedCourse designated = designatedCourse("C101", "자료구조", 3, 0);

      DesignatedCourseEvaluator.Evaluation result =
          evaluator.evaluate(List.of(designated), List.of(studentCourse("C101", gradeType)));

      assertThat(result.designatedCourses().get(0).status())
          .isEqualTo(DesignatedCourseCompletionStatus.NOT_COMPLETED);
    }

    StudentCourse deleted = studentCourse("C101", GradeType.A0);
    when(deleted.isRetakeDeleted()).thenReturn(true);

    DesignatedCourseEvaluator.Evaluation result =
        evaluator.evaluate(List.of(designatedCourse("C101", "자료구조", 3, 0)), List.of(deleted));

    assertThat(result.designatedCourses().get(0).status())
        .isEqualTo(DesignatedCourseCompletionStatus.NOT_COMPLETED);
  }

  @Test
  @DisplayName("코드가 없는 레거시 지정과목은 UNKNOWN으로 반환한다")
  void marksLegacyDesignatedCourseWithoutCodeAsUnknown() {
    DesignatedCourseEvaluator.Evaluation result =
        evaluator.evaluate(
            List.of(designatedCourse(" ", "자료구조", 3, 0)),
            List.of(studentCourse("C101", GradeType.A0)));

    assertThat(result.designatedCourses().get(0).status())
        .isEqualTo(DesignatedCourseCompletionStatus.UNKNOWN);
  }

  @Test
  @DisplayName("편입 인정학점은 코드별로 중복 없이 최대 학점을 합산한다")
  void sumsRecognizedTransferCreditsOncePerCode() {
    DesignatedCourseEvaluator.Evaluation result =
        evaluator.evaluate(
            List.of(),
            List.of(
                studentCourse("07045", GradeType.P, 15, false),
                studentCourse("07045", GradeType.P, 18, false),
                studentCourse("07050", GradeType.P, 17, false),
                studentCourse("C101", GradeType.P, 3, false),
                studentCourse("07046", GradeType.F, 20, false)));

    assertThat(result.recognizedTransferCredits()).isEqualTo(35);
  }

  @Test
  @DisplayName("수강 학점이 없으면 편입 인정학점을 미확인으로 둔다")
  void keepsRecognizedTransferCreditsUnknownWhenStudentCoursePointsAreMissing() {
    Course course = mock(Course.class);
    when(course.getCourseCode()).thenReturn("07045");
    CourseOffering offering = mock(CourseOffering.class);
    when(offering.getCourse()).thenReturn(course);
    when(offering.getPoints()).thenReturn(20);
    StudentCourse studentCourse = mock(StudentCourse.class);
    when(studentCourse.getOffering()).thenReturn(offering);
    when(studentCourse.getGrade()).thenReturn(new Grade(GradeType.P));
    when(studentCourse.getPoints()).thenReturn(null);
    when(studentCourse.isRetakeDeleted()).thenReturn(false);

    DesignatedCourseEvaluator.Evaluation result =
        evaluator.evaluate(List.of(), List.of(studentCourse));

    assertThat(result.recognizedTransferCredits()).isNull();
  }

  private StudentDesignatedCourse designatedCourse(
      String code, String name, Integer credits, int sourceOrder) {
    StudentDesignatedCourse designated = mock(StudentDesignatedCourse.class);
    when(designated.getSubjtCd()).thenReturn(code);
    when(designated.getSubjtNm()).thenReturn(name);
    when(designated.getPoint()).thenReturn(credits);
    when(designated.getSourceOrder()).thenReturn(sourceOrder);
    return designated;
  }

  private StudentCourse studentCourse(String code, GradeType gradeType) {
    return studentCourse(code, gradeType, 3, false);
  }

  private StudentCourse studentCourse(
      String code, GradeType gradeType, Integer points, boolean retakeDeleted) {
    Course course = mock(Course.class);
    when(course.getCourseCode()).thenReturn(code);
    CourseOffering offering = mock(CourseOffering.class);
    when(offering.getCourse()).thenReturn(course);
    StudentCourse studentCourse = mock(StudentCourse.class);
    when(studentCourse.getOffering()).thenReturn(offering);
    when(studentCourse.getGrade()).thenReturn(new Grade(gradeType));
    when(studentCourse.getPoints()).thenReturn(points);
    when(studentCourse.isRetakeDeleted()).thenReturn(retakeDeleted);
    return studentCourse;
  }
}
