package com.chukchuk.haksa.domain.lectureevaluations.service;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.lectureevaluations.config.LectureEvaluationProperties;
import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationRepository;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureEvaluationService {

    private final StudentService studentService;
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseEvaluationRepository courseEvaluationRepository;
    private final LectureEvaluationProperties properties;

    public LectureEvaluationDto.RequiredResponse getRequired(UUID userId) {
        Student student = studentService.getStudentByUserId(userId);
        UUID studentId = student.getId();
        Integer year = properties.getTargetYear();
        Integer semester = properties.getTargetSemester();

        SemesterAcademicRecord semesterRecord = semesterAcademicRecordRepository
                .findByStudentIdAndYearAndSemester(studentId, year, semester)
                .orElse(null);

        if (semesterRecord == null || !isPending(semesterRecord)) {
            LectureEvaluationStatus status = semesterRecord != null ? semesterRecord.getLectureEvaluationStatus() : null;
            return LectureEvaluationDto.RequiredResponse.withoutGrades(status, year, semester);
        }

        List<LectureEvaluationDto.GradeCard> grades = findEvaluationTargets(studentId, year, semester).stream()
                .map(LectureEvaluationDto.GradeCard::from)
                .toList();

        return new LectureEvaluationDto.RequiredResponse(LectureEvaluationStatus.PENDING, year, semester, grades);
    }

    @Transactional
    public void submit(UUID userId, LectureEvaluationDto.SubmitRequest request) {
        Student student = studentService.getStudentByUserId(userId);
        validateTargetSemester(request);

        SemesterAcademicRecord semesterRecord = semesterAcademicRecordRepository
                .findByStudentIdAndYearAndSemester(student.getId(), request.year(), request.semester())
                .orElseThrow(() -> new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED));

        if (!isPending(semesterRecord)) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED);
        }

        List<StudentCourse> targets = findEvaluationTargets(student.getId(), request.year(), request.semester());
        validateSubmittedCourses(targets, request.evaluations());

        Map<CourseProfessorKey, StudentCourse> targetMap = targets.stream()
                .collect(Collectors.toMap(CourseProfessorKey::from, Function.identity()));

        List<CourseEvaluation> evaluations = request.evaluations().stream()
                .map(submitted -> toEntity(student, request.year(), request.semester(), targetMap, submitted))
                .toList();

        courseEvaluationRepository.saveAll(evaluations);
        semesterRecord.markLectureEvaluationCompleted();
    }

    @Transactional
    public void skip(UUID userId, LectureEvaluationDto.SkipRequest request) {
        Student student = studentService.getStudentByUserId(userId);
        validateTargetSemester(request.year(), request.semester());

        SemesterAcademicRecord semesterRecord = semesterAcademicRecordRepository
                .findByStudentIdAndYearAndSemester(student.getId(), request.year(), request.semester())
                .orElseThrow(() -> new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED));

        if (!isPending(semesterRecord)) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED);
        }

        semesterRecord.markLectureEvaluationSkipped();
    }

    private void validateTargetSemester(LectureEvaluationDto.SubmitRequest request) {
        validateTargetSemester(request.year(), request.semester());
    }

    private void validateTargetSemester(Integer year, Integer semester) {
        if (!properties.getTargetYear().equals(year) || !properties.getTargetSemester().equals(semester)) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED);
        }
    }

    private boolean isPending(SemesterAcademicRecord semesterRecord) {
        return semesterRecord.isLectureEvaluationPending();
    }

    private List<StudentCourse> findEvaluationTargets(UUID studentId, Integer year, Integer semester) {
        return studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, year, semester).stream()
                .filter(this::isCompletedGrade)
                .filter(this::hasProfessor)
                .toList();
    }

    private boolean isCompletedGrade(StudentCourse studentCourse) {
        return studentCourse.getGrade() != null
                && studentCourse.getGrade().getValue() != null
                && studentCourse.getGrade().getValue() != GradeType.IP;
    }

    private boolean hasProfessor(StudentCourse studentCourse) {
        return studentCourse.getOffering() != null
                && studentCourse.getOffering().getProfessor() != null
                && studentCourse.getOffering().getProfessor().getId() != null;
    }

    private void validateSubmittedCourses(
            List<StudentCourse> targets,
            List<LectureEvaluationDto.SubmitEvaluation> submitted
    ) {
        Set<CourseProfessorKey> targetKeys = new HashSet<>(targets.stream()
                .map(CourseProfessorKey::from)
                .toList());
        Set<CourseProfessorKey> submittedKeys = new HashSet<>(submitted.stream()
                .map(CourseProfessorKey::from)
                .toList());

        if (targetKeys.size() != targets.size()
                || submittedKeys.size() != submitted.size()
                || !targetKeys.equals(submittedKeys)) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_COURSE_MISMATCH);
        }
    }

    private CourseEvaluation toEntity(
            Student student,
            Integer year,
            Integer semester,
            Map<CourseProfessorKey, StudentCourse> targetMap,
            LectureEvaluationDto.SubmitEvaluation submitted
    ) {
        StudentCourse target = targetMap.get(CourseProfessorKey.from(submitted));
        if (target == null) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_COURSE_MISMATCH);
        }

        return new CourseEvaluation(
                student,
                target.getOffering().getCourse(),
                target.getOffering().getProfessor(),
                year,
                semester,
                submitted.review(),
                submitted.selectedTags()
        );
    }

    private record CourseProfessorKey(Long courseId, Long professorId) {
        static CourseProfessorKey from(StudentCourse studentCourse) {
            return new CourseProfessorKey(
                    studentCourse.getOffering().getCourse().getId(),
                    studentCourse.getOffering().getProfessor() != null
                            ? studentCourse.getOffering().getProfessor().getId()
                            : null
            );
        }

        static CourseProfessorKey from(LectureEvaluationDto.SubmitEvaluation evaluation) {
            return new CourseProfessorKey(evaluation.courseId(), evaluation.professorId());
        }
    }
}
