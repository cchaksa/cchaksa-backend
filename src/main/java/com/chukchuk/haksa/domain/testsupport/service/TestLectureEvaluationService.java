package com.chukchuk.haksa.domain.testsupport.service;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import com.chukchuk.haksa.domain.lectureevaluations.model.LectureEvaluationTag;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationRepository;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationTagRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개발 환경의 강의평가 테스트 학기를 시나리오별 상태로 재구성한다. */
@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Transactional
public class TestLectureEvaluationService {

  private static final UUID TARGET_USER_ID =
      UUID.fromString("41c256af-1848-4691-bae5-72d2265c17d9");
  private static final UUID TARGET_STUDENT_ID =
      UUID.fromString("47f72b79-a3f0-4834-869b-8ba3a0cf3474");
  private static final int TARGET_YEAR = 2026;
  private static final int TARGET_SEMESTER = 10;

  private final UserRepository userRepository;
  private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
  private final StudentCourseRepository studentCourseRepository;
  private final CourseOfferingRepository courseOfferingRepository;
  private final CourseEvaluationRepository courseEvaluationRepository;
  private final CourseEvaluationTagRepository courseEvaluationTagRepository;
  private final AcademicCache academicCache;

  /** 대상 테스트 학생의 강의평가 학기 데이터를 모두 제거하고 학사 캐시를 비운다. */
  public void setEmptySemester() {
    Student student = getTargetStudent();
    clearTargetSemester(student.getId());
    academicCache.deleteAllByStudentId(student.getId());
  }

  /** 대상 학기를 성적 미공개 상태의 테스트 데이터로 재구성한다. */
  public void setNotReleased() {
    rebuildSemester(LectureEvaluationStatus.NOT_RELEASED, GradeType.IP, false);
  }

  /** 대상 학기를 강의평가 제출 대기 상태의 테스트 데이터로 재구성한다. */
  public void setPending() {
    rebuildSemester(LectureEvaluationStatus.PENDING, GradeType.A_PLUS, false);
  }

  /** 대상 학기를 강의평가 건너뛰기 완료 상태의 테스트 데이터로 재구성한다. */
  public void setSkipped() {
    rebuildSemester(LectureEvaluationStatus.SKIPPED, GradeType.A_PLUS, false);
  }

  /** 대상 학기를 평가 내역이 저장된 강의평가 완료 상태로 재구성한다. */
  public void setCompleted() {
    rebuildSemester(LectureEvaluationStatus.COMPLETED, GradeType.A_PLUS, true);
  }

  private void rebuildSemester(
      LectureEvaluationStatus status, GradeType gradeType, boolean createEvaluations) {
    Student student = getTargetStudent();
    clearTargetSemester(student.getId());

    List<CourseOffering> offerings = findReusableOfferings();
    semesterAcademicRecordRepository.save(newSemesterRecord(student, status, offerings));
    studentCourseRepository.saveAll(toStudentCourses(student, offerings, gradeType));
    if (createEvaluations) {
      courseEvaluationRepository.saveAll(toCourseEvaluations(student, offerings));
    }
    academicCache.deleteAllByStudentId(student.getId());
  }

  private Student getTargetStudent() {
    User user =
        userRepository
            .findById(TARGET_USER_ID)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    Student student = user.getStudent();
    if (student == null || !TARGET_STUDENT_ID.equals(student.getId())) {
      throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
    }
    return student;
  }

  private void clearTargetSemester(UUID studentId) {
    courseEvaluationTagRepository.deleteByStudentIdAndYearAndSemester(
        studentId, TARGET_YEAR, TARGET_SEMESTER);
    courseEvaluationRepository.deleteByStudentIdAndYearAndSemester(
        studentId, TARGET_YEAR, TARGET_SEMESTER);
    studentCourseRepository.deleteByStudentIdAndYearAndSemester(
        studentId, TARGET_YEAR, TARGET_SEMESTER);
    semesterAcademicRecordRepository.deleteByStudentIdAndYearAndSemester(
        studentId, TARGET_YEAR, TARGET_SEMESTER);
  }

  private List<CourseOffering> findReusableOfferings() {
    courseOfferingRepository.normalizeUnsupportedEvaluationTypes(TARGET_YEAR, TARGET_SEMESTER);
    List<CourseOffering> offerings =
        courseOfferingRepository.findReusableLectureEvaluationTestOfferings(
            TARGET_YEAR, TARGET_SEMESTER);
    if (offerings.isEmpty()) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
    return offerings;
  }

  private SemesterAcademicRecord newSemesterRecord(
      Student student, LectureEvaluationStatus status, List<CourseOffering> offerings) {
    int attemptedCredits =
        offerings.stream()
            .map(CourseOffering::getPoints)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    SemesterAcademicRecord record =
        new SemesterAcademicRecord(
            student,
            TARGET_YEAR,
            TARGET_SEMESTER,
            attemptedCredits,
            status == LectureEvaluationStatus.NOT_RELEASED ? 0 : attemptedCredits,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            null);
    record.setLectureEvaluationStatusForTest(status);
    return record;
  }

  private List<StudentCourse> toStudentCourses(
      Student student, List<CourseOffering> offerings, GradeType gradeType) {
    return offerings.stream()
        .map(
            offering ->
                new StudentCourse(
                    student,
                    offering,
                    new Grade(gradeType),
                    offering.getPoints(),
                    false,
                    gradeType == GradeType.IP ? null : 95,
                    false))
        .toList();
  }

  private List<CourseEvaluation> toCourseEvaluations(
      Student student, List<CourseOffering> offerings) {
    return offerings.stream()
        .map(
            offering ->
                new CourseEvaluation(
                    student,
                    offering.getCourse(),
                    offering.getProfessor(),
                    TARGET_YEAR,
                    TARGET_SEMESTER,
                    "프론트 테스트용 강의평가입니다.",
                    List.of(LectureEvaluationTag.INTERESTING_LECTURE)))
        .toList();
  }
}
