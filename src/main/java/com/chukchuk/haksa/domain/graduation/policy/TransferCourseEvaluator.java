// 편입생 수강 기록을 유효 과목과 학점 집계로 정규화한다.

package com.chukchuk.haksa.domain.graduation.policy;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 편입생 분석에서만 사용하는 유효 수강 기록 평가기다. */
@Component
public class TransferCourseEvaluator {

  private static final Set<String> TRANSFER_CREDIT_CODES =
      Set.of("07045", "07046", "00111", "07050");
  private static final Set<GradeType> NON_PASSING_GRADES =
      Set.of(GradeType.F, GradeType.R, GradeType.NP, GradeType.IP);

  /**
   * 유효한 수강 기록을 과목 코드별로 한 번씩 정규화한다.
   *
   * @param studentCourses 학생의 전체 수강 기록
   * @return 영역 계산과 지정과목 계산에 함께 사용할 평가 결과
   */
  public Evaluation evaluate(List<StudentCourse> studentCourses) {
    Map<String, StudentCourse> latestByCourseCode = new HashMap<>();
    Map<String, Integer> recognizedCredits = new HashMap<>();
    Set<String> unknownCreditCourseCodes = new LinkedHashSet<>();
    Set<String> conflictingCourseCodes = new LinkedHashSet<>();
    List<CourseInternalDto> uncodedCourses = new ArrayList<>();

    for (StudentCourse studentCourse :
        studentCourses == null ? Collections.<StudentCourse>emptyList() : studentCourses) {
      if (!isValid(studentCourse)) {
        continue;
      }

      String courseCode = normalizeCode(courseCodeOf(studentCourse));
      if (courseCode == null) {
        uncodedCourses.add(toInternal(null, studentCourse));
        continue;
      }

      if (TRANSFER_CREDIT_CODES.contains(courseCode)) {
        Integer points = studentCourse.getPoints();
        if (points == null) {
          unknownCreditCourseCodes.add(courseCode);
        } else {
          recognizedCredits.merge(courseCode, points, Math::max);
        }
      }

      StudentCourse previous = latestByCourseCode.get(courseCode);
      int recency = previous == null ? 1 : compareRecency(studentCourse, previous);
      if (previous == null || recency > 0) {
        latestByCourseCode.put(courseCode, studentCourse);
        conflictingCourseCodes.remove(courseCode);
      } else if (recency == 0 && !sameResult(studentCourse, previous)) {
        conflictingCourseCodes.add(courseCode);
      }
    }
    unknownCreditCourseCodes.addAll(conflictingCourseCodes);

    List<CourseInternalDto> courses =
        latestByCourseCode.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> toInternal(entry.getKey(), entry.getValue()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    courses.addAll(uncodedCourses);

    Map<String, Integer> creditsByCourseCode = new LinkedHashMap<>();
    Map<String, Integer> earnedCreditsByCourseCode = new LinkedHashMap<>();
    for (CourseInternalDto course : courses) {
      if (course.getCredits() == null) {
        if (course.getCourseCode() != null) {
          unknownCreditCourseCodes.add(course.getCourseCode());
        }
      } else {
        if (course.getCourseCode() != null) {
          creditsByCourseCode.put(course.getCourseCode(), course.getCredits());
          if (!TRANSFER_CREDIT_CODES.contains(course.getCourseCode())) {
            earnedCreditsByCourseCode.put(course.getCourseCode(), course.getCredits());
          }
        }
      }
    }

    Integer recognizedTransferCredits =
        unknownCreditCourseCodes.stream().anyMatch(TRANSFER_CREDIT_CODES::contains)
            ? null
            : recognizedCredits.values().stream().mapToInt(Integer::intValue).sum();

    return new Evaluation(
        courses,
        creditsByCourseCode,
        earnedCreditsByCourseCode,
        unknownCreditCourseCodes,
        recognizedTransferCredits);
  }

  private CourseInternalDto toInternal(String courseCode, StudentCourse studentCourse) {
    CourseOffering offering = studentCourse.getOffering();
    Grade grade = studentCourse.getGrade();
    return new CourseInternalDto(
        offering.getId(),
        offering.getFacultyDivisionName() == null ? null : offering.getFacultyDivisionName().name(),
        studentCourse.getPoints(),
        grade == null || grade.getValue() == null ? null : grade.getValue().getValue(),
        offering.getCourse().getCourseName(),
        offering.getSemester(),
        offering.getYear(),
        courseCode,
        studentCourse.getOriginalScore(),
        offering.getLiberalArtsAreaCode() == null
            ? null
            : offering.getLiberalArtsAreaCode().getCode());
  }

  private int compareRecency(StudentCourse left, StudentCourse right) {
    CourseOffering leftOffering = left.getOffering();
    CourseOffering rightOffering = right.getOffering();
    int year =
        Integer.compare(nullToZero(leftOffering.getYear()), nullToZero(rightOffering.getYear()));
    if (year != 0) {
      return year;
    }
    return Integer.compare(
        nullToZero(leftOffering.getSemester()), nullToZero(rightOffering.getSemester()));
  }

  private boolean sameResult(StudentCourse left, StudentCourse right) {
    return Objects.equals(left.getPoints(), right.getPoints())
        && Objects.equals(left.getGrade().getValue(), right.getGrade().getValue())
        && Objects.equals(
            left.getOffering().getFacultyDivisionName(),
            right.getOffering().getFacultyDivisionName());
  }

  private boolean isValid(StudentCourse studentCourse) {
    if (studentCourse == null || studentCourse.getOffering() == null) {
      return false;
    }
    if (studentCourse.getOffering().getCourse() == null) {
      return false;
    }
    Grade grade = studentCourse.getGrade();
    return grade != null
        && grade.getValue() != null
        && !NON_PASSING_GRADES.contains(grade.getValue())
        && !studentCourse.isRetakeDeleted();
  }

  private String courseCodeOf(StudentCourse studentCourse) {
    return studentCourse.getOffering().getCourse() == null
        ? null
        : studentCourse.getOffering().getCourse().getCourseCode();
  }

  private String normalizeCode(String courseCode) {
    if (courseCode == null || courseCode.isBlank()) {
      return null;
    }
    return courseCode.trim().toUpperCase(Locale.ROOT);
  }

  private int nullToZero(Integer value) {
    return value == null ? 0 : value;
  }

  /**
   * 편입생 수강 기록 평가 결과다.
   *
   * @param courses 코드별로 정규화한 전체 이수 과목
   * @param creditsByCourseCode 코드별 개인 취득학점
   * @param earnedCreditsByCourseCode 인정학점을 제외한 코드별 개인 취득학점
   * @param unknownCreditCourseCodes 개인 취득학점을 확인할 수 없는 과목 코드
   * @param recognizedTransferCredits 편입 인정학점 합계
   */
  public record Evaluation(
      List<CourseInternalDto> courses,
      Map<String, Integer> creditsByCourseCode,
      Map<String, Integer> earnedCreditsByCourseCode,
      Set<String> unknownCreditCourseCodes,
      Integer recognizedTransferCredits) {

    /**
     * 결과 목록과 집계를 방어적으로 보관한다.
     *
     * @param courses 코드별로 정규화한 전체 이수 과목
     * @param creditsByCourseCode 코드별 개인 취득학점
     * @param earnedCreditsByCourseCode 인정학점을 제외한 코드별 개인 취득학점
     * @param unknownCreditCourseCodes 개인 취득학점을 확인할 수 없는 과목 코드
     * @param recognizedTransferCredits 편입 인정학점 합계
     */
    public Evaluation {
      courses = List.copyOf(new ArrayList<>(courses));
      creditsByCourseCode = Map.copyOf(new LinkedHashMap<>(creditsByCourseCode));
      earnedCreditsByCourseCode = Map.copyOf(new LinkedHashMap<>(earnedCreditsByCourseCode));
      unknownCreditCourseCodes = Set.copyOf(new LinkedHashSet<>(unknownCreditCourseCodes));
    }
  }
}
