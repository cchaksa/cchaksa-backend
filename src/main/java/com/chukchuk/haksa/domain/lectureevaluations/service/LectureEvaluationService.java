package com.chukchuk.haksa.domain.lectureevaluations.service;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.lectureevaluations.config.LectureEvaluationProperties;
import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import com.chukchuk.haksa.domain.lectureevaluations.model.LectureEvaluationTag;
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
import java.util.Set;
import java.util.UUID;

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
            return LectureEvaluationDto.RequiredResponse.notRequired(year, semester);
        }

        List<LectureEvaluationDto.GradeCard> grades = findEvaluationTargets(studentId, year, semester).stream()
                .map(LectureEvaluationDto.GradeCard::from)
                .toList();

        return new LectureEvaluationDto.RequiredResponse(true, year, semester, grades);
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

        List<CourseEvaluation> evaluations = request.evaluations().stream()
                .map(submitted -> toEntity(student, request.year(), request.semester(), targets, submitted))
                .toList();

        courseEvaluationRepository.saveAll(evaluations);
        semesterRecord.markLectureEvaluationCompleted();
    }

    private void validateTargetSemester(LectureEvaluationDto.SubmitRequest request) {
        if (!properties.getTargetYear().equals(request.year()) || !properties.getTargetSemester().equals(request.semester())) {
            throw new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED);
        }
    }

    private boolean isPending(SemesterAcademicRecord semesterRecord) {
        return semesterRecord.isLectureEvaluationRequired() && !semesterRecord.isLectureEvaluationCompleted();
    }

    private List<StudentCourse> findEvaluationTargets(UUID studentId, Integer year, Integer semester) {
        return studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, year, semester).stream()
                .filter(this::isCompletedGrade)
                .toList();
    }

    private boolean isCompletedGrade(StudentCourse studentCourse) {
        return studentCourse.getGrade() != null
                && studentCourse.getGrade().getValue() != null
                && studentCourse.getGrade().getValue() != GradeType.IP;
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
            List<StudentCourse> targets,
            LectureEvaluationDto.SubmitEvaluation submitted
    ) {
        StudentCourse target = targets.stream()
                .filter(studentCourse -> CourseProfessorKey.from(studentCourse).equals(CourseProfessorKey.from(submitted)))
                .findFirst()
                .orElseThrow(() -> new CommonException(ErrorCode.LECTURE_EVALUATION_COURSE_MISMATCH));

        return new CourseEvaluation(
                student,
                target.getOffering().getCourse(),
                target.getOffering().getProfessor(),
                year,
                semester,
                submitted.review(),
                parseTags(submitted.selectedTags())
        );
    }

    private List<LectureEvaluationTag> parseTags(List<String> selectedTags) {
        return selectedTags.stream()
                .map(LectureEvaluationTag::valueOf)
                .toList();
    }

    private record CourseProfessorKey(Long courseId, Long professorId) {
        static CourseProfessorKey from(StudentCourse studentCourse) {
            return new CourseProfessorKey(
                    studentCourse.getOffering().getCourse().getId(),
                    studentCourse.getOffering().getProfessor().getId()
            );
        }

        static CourseProfessorKey from(LectureEvaluationDto.SubmitEvaluation evaluation) {
            return new CourseProfessorKey(evaluation.courseId(), evaluation.professorId());
        }
    }
}
