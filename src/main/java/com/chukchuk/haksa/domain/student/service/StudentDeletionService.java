package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDeletionService {

    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void deleteByStudent(Student student) {
        if (student == null) {
            log.warn("[BIZ] student.deletion.skip, reason=unconnected_user");
            return;
        }

        UUID studentId = student.getId();
        if (studentId != null) {
            studentCourseRepository.deleteByStudentId(studentId);
            semesterAcademicRecordRepository.deleteByStudentId(studentId);
            studentAcademicRecordRepository.deleteByStudentId(studentId);
        }
        studentRepository.delete(student);
    }
}
