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

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class AcademicRecordRepository {
    private final StudentRepository studentRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;

    /** 포털 최초 연동 */
    @Transactional
    public void insertAllAcademicRecords(AcademicRecord academicRecord, Student student) {
        UUID studentId = student.getId();

        // StudentAcademicRecord upsert
        studentAcademicRecordRepository.findByStudentId(studentId)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateWith(academicRecord.getSummary());
                            student.setAcademicRecord(existing);
                            // 영속 상태면 save 생략 가능하지만 명시 호출 무해
                            studentAcademicRecordRepository.save(existing);
                        },
                        () -> {
                            StudentAcademicRecord newRecord =
                                    AcademicRecordMapper.toEntity(student, academicRecord.getSummary());
                            student.setAcademicRecord(newRecord);
                            studentAcademicRecordRepository.save(newRecord);
                        }
                );

        // SemesterAcademicRecord upsert (초기 연동이라도 재시도 상황 대비)
        List<SemesterAcademicRecord> incoming = academicRecord.getSemesters().stream()
                .map(g -> AcademicRecordMapper.toEntity(student, g))
                .toList();

        List<SemesterAcademicRecord> existingList = semesterAcademicRecordRepository.findByStudentId(studentId);
        Map<String, SemesterAcademicRecord> existingByKey = indexByKey(existingList);

        List<SemesterAcademicRecord> toInsert = new ArrayList<>();
        List<SemesterAcademicRecord> toUpdate = new ArrayList<>();

        for (SemesterAcademicRecord in : incoming) {
            String k = key(in.getYear(), in.getSemester());
            SemesterAcademicRecord cur = existingByKey.get(k);
            if (cur == null) {
                // 없으면 insert
                student.addSemesterRecord(in);
                toInsert.add(in);
            } else if (!cur.equalsContentOf(in)) {
                // 있으면 내용이 바뀐 경우에만 업데이트
                cur.updateWith(in);
                toUpdate.add(cur);
            }
        }

        if (!toInsert.isEmpty()) {
            semesterAcademicRecordRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            semesterAcademicRecordRepository.saveAll(toUpdate);
        }

        studentRepository.save(student);
    }

    /** 포털 재연동 */
    @Transactional
    public void updateChangedAcademicRecords(AcademicRecord academicRecord, Student student) {
        UUID studentId = student.getId();

        // 1) 요약 upsert
        StudentAcademicRecord summary = studentAcademicRecordRepository.findByStudentId(studentId)
                .map(existing -> {
                    if (!existing.isSameAs(academicRecord.getSummary())) {
                        existing.updateWith(academicRecord.getSummary());
                        studentAcademicRecordRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    StudentAcademicRecord newRecord =
                            AcademicRecordMapper.toEntity(student, academicRecord.getSummary());
                    studentAcademicRecordRepository.save(newRecord);
                    return newRecord;
                });
        student.setAcademicRecord(summary);

        // 2) 학기별 upsert
        List<SemesterAcademicRecord> incoming = academicRecord.getSemesters().stream()
                .map(g -> AcademicRecordMapper.toEntity(student, g))
                .toList();

        List<SemesterAcademicRecord> existingList = semesterAcademicRecordRepository.findByStudentId(studentId);
        Map<String, SemesterAcademicRecord> existingByKey = indexByKey(existingList);

        List<SemesterAcademicRecord> toInsert = new ArrayList<>();
        List<SemesterAcademicRecord> toUpdate = new ArrayList<>();

        for (SemesterAcademicRecord in : incoming) {
            String k = key(in.getYear(), in.getSemester());
            SemesterAcademicRecord cur = existingByKey.get(k);
            if (cur == null) {
                student.addSemesterRecord(in);
                toInsert.add(in);
            } else if (!cur.equalsContentOf(in)) {
                cur.updateWith(in);
                toUpdate.add(cur);
            }
        }

        if (!toInsert.isEmpty()) {
            semesterAcademicRecordRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            semesterAcademicRecordRepository.saveAll(toUpdate);
        }
    }

    /* ===== helpers ===== */

    private Map<String, SemesterAcademicRecord> indexByKey(List<SemesterAcademicRecord> list) {
        return list.stream().collect(Collectors.toMap(
                e -> key(e.getYear(), e.getSemester()),
                e -> e
        ));
    }

    private String key(int year, int semester) {
        return year + "-" + semester;
    }
}