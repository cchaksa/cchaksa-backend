// 3학년 편입생의 졸업요건을 저장된 학사 데이터로 분석한다.

package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.academic.record.service.StudentAcademicRecordService;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseCompletionStatus;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.dto.TransferAreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.TransferGraduationProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.TransferManualReviewReason;
import com.chukchuk.haksa.domain.graduation.policy.DesignatedCourseEvaluator;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.graduation.policy.MajorResolutionResult;
import com.chukchuk.haksa.domain.graduation.policy.TransferAreaEvaluator;
import com.chukchuk.haksa.domain.graduation.policy.TransferCourseEvaluator;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 편입 인정학점과 편입생 전용 졸업요건을 함께 계산한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferGraduationAnalysisService {

  private static final int REQUIRED_TOTAL_CREDITS = 130;
  private static final BigDecimal DEFAULT_REQUIRED_GPA = new BigDecimal("2.0");
  private static final BigDecimal SECONDARY_MAJOR_REQUIRED_GPA = new BigDecimal("2.5");

  private final StudentAcademicRecordService studentAcademicRecordService;
  private final StudentCourseRepository studentCourseRepository;
  private final StudentDesignatedCourseRepository studentDesignatedCourseRepository;
  private final StudentGraduationProgressService studentGraduationProgressService;
  private final DesignatedCourseEvaluator designatedCourseEvaluator;
  private final TransferCourseEvaluator transferCourseEvaluator;
  private final TransferAreaEvaluator transferAreaEvaluator;
  private final GraduationMajorResolver graduationMajorResolver;
  private final GraduationQueryRepository graduationQueryRepository;

  /**
   * 편입생의 현재 데이터로 계산 가능한 이수 현황을 API 응답으로 변환한다.
   *
   * @param student 분석할 편입생
   * @return 편입생 부분 진단 응답
   */
  public GraduationProgressResponse analyze(Student student) {
    UUID studentId = student.getId();
    List<TransferManualReviewReason> reviewReasons = baseManualReviewReasons();

    StudentAcademicRecord academicRecord =
        studentAcademicRecordService.findStudentAcademicRecordByStudentId(studentId).orElse(null);
    Integer totalEarnedCredits =
        academicRecord == null ? null : academicRecord.getTotalEarnedCredits();
    BigDecimal cumulativeGpa = academicRecord == null ? null : academicRecord.getCumulativeGpa();
    if (totalEarnedCredits == null || cumulativeGpa == null) {
      reviewReasons.add(TransferManualReviewReason.ACADEMIC_SUMMARY_INCOMPLETE);
    }

    List<StudentDesignatedCourse> designatedCourses =
        studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(studentId);
    List<StudentCourse> studentCourses =
        studentCourseRepository.findAllWithCourseByStudentId(studentId);
    TransferCourseEvaluator.Evaluation courseEvaluation =
        transferCourseEvaluator.evaluate(studentCourses);
    DesignatedCourseEvaluator.Evaluation designatedEvaluation =
        designatedCourseEvaluator.evaluate(designatedCourses, courseEvaluation);
    if (designatedEvaluation == null) {
      designatedEvaluation = designatedCourseEvaluator.evaluate(designatedCourses, studentCourses);
    }
    if (designatedEvaluation == null) {
      designatedEvaluation =
          new DesignatedCourseEvaluator.Evaluation(
              List.of(), courseEvaluation.recognizedTransferCredits());
    }

    List<TransferAreaProgressDto> areas =
        transferAreaEvaluator.evaluate(courseEvaluation, resolveRequirements(student));
    if (areas == null) {
      areas = List.of();
    }
    addUnavailableAreaReasons(areas, reviewReasons);
    if (courseEvaluation.recognizedTransferCredits() == null) {
      reviewReasons.add(TransferManualReviewReason.RECOGNIZED_CREDITS_INCOMPLETE);
    }

    boolean designatedCoursesNeedsRefresh = student.getDesignatedCoursesSnapshotVersion() == null;
    if (designatedCoursesNeedsRefresh) {
      reviewReasons.add(TransferManualReviewReason.DESIGNATED_COURSES_NOT_VERIFIED);
    }

    Boolean languageCertFulfilled =
        studentGraduationProgressService.getLanguageCertFulfilled(studentId).orElse(null);
    if (languageCertFulfilled == null) {
      reviewReasons.add(TransferManualReviewReason.LANGUAGE_CERT_NOT_VERIFIED);
    }

    DesignatedCreditResult designatedCreditResult =
        designatedCoursesNeedsRefresh
            ? new DesignatedCreditResult(null, List.of("SNAPSHOT_NOT_RECEIVED"))
            : calculateDesignatedEarnedCredits(designatedEvaluation, courseEvaluation);
    TransferGraduationProgressDto transferProgress =
        new TransferGraduationProgressDto(
            REQUIRED_TOTAL_CREDITS,
            totalEarnedCredits,
            remainingCredits(totalEarnedCredits),
            fulfillCredits(totalEarnedCredits),
            courseEvaluation.recognizedTransferCredits(),
            cumulativeGpa,
            requiredGpa(student),
            fulfillGpa(student, cumulativeGpa),
            student.getAcademicInfo() == null
                ? null
                : student.getAcademicInfo().getCompletedSemesters(),
            designatedCoursesNeedsRefresh,
            designatedEvaluation.designatedCourses(),
            true,
            reviewReasons,
            areas,
            designatedCreditResult.earnedCredits(),
            designatedCreditResult.unavailableReasons());

    return GraduationProgressResponse.forTransfer(transferProgress, languageCertFulfilled);
  }

  private List<TransferManualReviewReason> baseManualReviewReasons() {
    return new ArrayList<>(
        List.of(
            TransferManualReviewReason.REGISTERED_SEMESTERS_NOT_VERIFIED,
            TransferManualReviewReason.MINOR_OR_LINKED_MAJOR_NOT_ASSESSABLE,
            TransferManualReviewReason.GRADUATION_REVIEW_NOT_AVAILABLE));
  }

  private TransferAreaEvaluator.Requirements resolveRequirements(Student student) {
    Integer transferYear =
        student.getAcademicInfo() == null ? null : student.getAcademicInfo().getAdmissionYear();
    if (transferYear == null
        || transferYear <= 2
        || (student.getMajor() == null && student.getDepartment() == null)
        || student.getSecondaryMajor() != null) {
      return TransferAreaEvaluator.Requirements.unavailable();
    }

    int curriculumYear = transferYear - 2;
    List<AreaRequirementDto> requirements;
    try {
      MajorResolutionResult major = graduationMajorResolver.resolve(student, curriculumYear);
      requirements =
          graduationQueryRepository.getAreaRequirementsWithCache(
              major.primaryMajorId(), curriculumYear);
    } catch (CommonException exception) {
      if (!ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.code().equals(exception.getCode())) {
        throw exception;
      }
      return TransferAreaEvaluator.Requirements.unavailable();
    }

    BigDecimal coreCredits = halfRequiredCredits(requirements, "전핵");
    BigDecimal electiveCredits = halfRequiredCredits(requirements, "전선");
    return new TransferAreaEvaluator.Requirements(
        coreCredits,
        electiveCredits,
        coreCredits == null ? List.of("CORE_CURRICULUM_UNAVAILABLE") : List.of(),
        electiveCredits == null ? List.of("ELECTIVE_REQUIREMENT_UNAVAILABLE") : List.of());
  }

  private BigDecimal halfRequiredCredits(List<AreaRequirementDto> requirements, String area) {
    List<Integer> credits =
        requirements.stream()
            .filter(requirement -> area.equals(requirement.areaType().trim()))
            .map(AreaRequirementDto::requiredCredits)
            .distinct()
            .toList();
    return credits.size() == 1
        ? BigDecimal.valueOf(credits.get(0)).multiply(new BigDecimal("0.5"))
        : null;
  }

  private void addUnavailableAreaReasons(
      List<TransferAreaProgressDto> areas, List<TransferManualReviewReason> reviewReasons) {
    areas.stream()
        .flatMap(area -> area.unavailableReasons().stream())
        .distinct()
        .forEach(
            reason -> {
              if ("CORE_CURRICULUM_UNAVAILABLE".equals(reason)) {
                reviewReasons.add(TransferManualReviewReason.REQUIRED_COURSES_NOT_ASSESSABLE);
              }
              if ("ELECTIVE_REQUIREMENT_UNAVAILABLE".equals(reason)) {
                reviewReasons.add(TransferManualReviewReason.ELECTIVE_RATIO_NOT_ASSESSABLE);
              }
            });
  }

  private DesignatedCreditResult calculateDesignatedEarnedCredits(
      DesignatedCourseEvaluator.Evaluation designatedEvaluation,
      TransferCourseEvaluator.Evaluation courseEvaluation) {
    Set<String> countedCodes = new java.util.HashSet<>();
    int earnedCredits = 0;
    for (DesignatedCourseProgressDto course : designatedEvaluation.designatedCourses()) {
      if (course.status() == DesignatedCourseCompletionStatus.UNKNOWN) {
        return new DesignatedCreditResult(null, List.of("COURSE_DATA_INCOMPLETE"));
      }
      if (course.status() != DesignatedCourseCompletionStatus.COMPLETED
          || course.courseCode() == null
          || !countedCodes.add(course.courseCode())) {
        continue;
      }
      Integer credits = courseEvaluation.creditsByCourseCode().get(course.courseCode());
      if (credits == null
          || courseEvaluation.unknownCreditCourseCodes().contains(course.courseCode())) {
        return new DesignatedCreditResult(null, List.of("COURSE_DATA_INCOMPLETE"));
      }
      earnedCredits += credits;
    }
    return new DesignatedCreditResult(earnedCredits, List.of());
  }

  private record DesignatedCreditResult(Integer earnedCredits, List<String> unavailableReasons) {}

  private Boolean fulfillCredits(Integer totalEarnedCredits) {
    return totalEarnedCredits == null ? null : totalEarnedCredits >= REQUIRED_TOTAL_CREDITS;
  }

  private Integer remainingCredits(Integer totalEarnedCredits) {
    return totalEarnedCredits == null
        ? null
        : Math.max(0, REQUIRED_TOTAL_CREDITS - totalEarnedCredits);
  }

  private Boolean fulfillGpa(Student student, BigDecimal cumulativeGpa) {
    return cumulativeGpa == null ? null : cumulativeGpa.compareTo(requiredGpa(student)) >= 0;
  }

  private BigDecimal requiredGpa(Student student) {
    return student.getSecondaryMajor() == null
        ? DEFAULT_REQUIRED_GPA
        : SECONDARY_MAJOR_REQUIRED_GPA;
  }
}
