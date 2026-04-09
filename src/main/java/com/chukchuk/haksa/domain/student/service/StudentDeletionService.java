package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
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
    private final StudentRepository studentRepository;

    @Transactional
    public void deleteByStudentId(UUID studentId) {
        if (studentId == null) {
            return;
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            log.warn("[BIZ] student.deletion.skip studentId={} reason=missing_student", studentId);
            return;
        }

        student.resetAcademicData();
        studentAcademicRecordRepository.deleteByStudentId(studentId);
        studentRepository.delete(student);
    }
}
