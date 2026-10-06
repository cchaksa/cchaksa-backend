// 탈퇴 시 지정과목과 학생 개인정보 정리 동작을 검증한다.

package com.chukchuk.haksa.domain.student.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentDeletionServiceUnitTests {

  @Mock private StudentAcademicRecordRepository studentAcademicRecordRepository;

  @Mock private SemesterAcademicRecordRepository semesterAcademicRecordRepository;

  @Mock private StudentCourseRepository studentCourseRepository;

  @Mock private StudentGraduationProgressRepository studentGraduationProgressRepository;

  @Mock private StudentDesignatedCourseRepository studentDesignatedCourseRepository;

  @Mock private StudentRepository studentRepository;

  @Mock private Student student;

  @InjectMocks private StudentDeletionService studentDeletionService;

  @Test
  @DisplayName("학생 탈퇴 시 지정과목과 스냅샷 버전을 함께 초기화한다")
  void anonymizeByStudentClearsDesignatedCourses() {
    UUID studentId = UUID.randomUUID();
    when(student.getId()).thenReturn(studentId);
    when(studentRepository.findForUpdateById(studentId)).thenReturn(Optional.of(student));

    studentDeletionService.anonymizeByStudent(student);

    verify(studentDesignatedCourseRepository).deleteAllByStudentId(studentId);
    verify(student).resetDesignatedCourseSnapshot(any(Instant.class));
    verify(student).anonymize();
    verify(studentRepository).saveAndFlush(student);
  }
}
