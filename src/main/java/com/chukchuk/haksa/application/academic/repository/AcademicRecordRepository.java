package com.chukchuk.haksa.application.academic.repository;

import com.chukchuk.haksa.application.academic.AcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.infrastructure.portal.mapper.AcademicRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class AcademicRecordRepository {
    private final StudentRepository studentRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;

    /**
     * 포털 최초 연동
     */
    @Transactional
    public void insertAllAcademicRecords(AcademicRecord academicRecord, Student student) {
        UUID studentId = student.getId();

        // StudentAcademicRecord insert or update
        studentAcademicRecordRepository.findByStudentId(studentId)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateWith(academicRecord.getSummary());
                            student.setAcademicRecord(existing);
                            studentAcademicRecordRepository.save(existing);
                        },
                        () -> {
                            StudentAcademicRecord newRecord = AcademicRecordMapper.toEntity(student, academicRecord.getSummary());
                            student.setAcademicRecord(newRecord);
                            studentAcademicRecordRepository.save(newRecord);
                        }
                );

        // SemesterAcademicRecord bulk insert
        List<SemesterAcademicRecord> semesters = academicRecord.getSemesters().stream()
                .map(g -> AcademicRecordMapper.toEntity(student, g))
                .toList();

        semesters.forEach(student::addSemesterRecord);
        semesterAcademicRecordRepository.saveAll(semesters);
        studentRepository.save(student);
    }

    /**
     * 포털 재연동
     */
    @Transactional
    public void updateChangedAcademicRecords(AcademicRecord academicRecord, Student student) {
        UUID studentId = student.getId();

        // 1. 요약 정보 변경 시에만 업데이트
        StudentAcademicRecord summary = studentAcademicRecordRepository.findByStudentId(studentId)
                .map(existing -> {
                    if (!existing.isSameAs(academicRecord.getSummary())) {
                        existing.updateWith(academicRecord.getSummary());
                        studentAcademicRecordRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    StudentAcademicRecord newRecord = AcademicRecordMapper.toEntity(student, academicRecord.getSummary());
                    studentAcademicRecordRepository.save(newRecord);
                    return newRecord;
                });

        student.setAcademicRecord(summary);

        // 2. 학기별 성적 비교 후 변경된 것만 저장
        List<SemesterAcademicRecord> newSemesters = academicRecord.getSemesters().stream()
                .map(g -> AcademicRecordMapper.toEntity(student, g))
                .toList();

        List<SemesterAcademicRecord> existing = semesterAcademicRecordRepository.findByStudentId(studentId);
        List<SemesterAcademicRecord> toSave = newSemesters.stream()
                .filter(s -> existing.stream().noneMatch(e -> e.equalsContentOf(s)))
                .toList();

        toSave.forEach(student::addSemesterRecord);
        semesterAcademicRecordRepository.saveAll(toSave);
    }
}