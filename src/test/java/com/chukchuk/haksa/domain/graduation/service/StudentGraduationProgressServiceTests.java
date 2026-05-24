// 학생 졸업 진행 정보의 외국어 인증 저장 정책을 검증하는 테스트
package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentGraduationProgressServiceTests {

    @Mock
    private StudentGraduationProgressRepository repository;

    @Mock
    private AcademicCache academicCache;

    @Mock
    private Student student;

    @Test
    @DisplayName("외국어 인증 row가 없으면 새로 생성하고 학생 캐시를 무효화한다")
    void syncLanguageCertCreatesProgressWhenMissing() {
        UUID studentId = UUID.randomUUID();
        StudentGraduationProgressService service = new StudentGraduationProgressService(repository, academicCache);

        when(student.getId()).thenReturn(studentId);
        when(repository.findByStudentId(studentId)).thenReturn(Optional.empty());

        service.syncLanguageCert(student, true);

        ArgumentCaptor<StudentGraduationProgress> captor = ArgumentCaptor.forClass(StudentGraduationProgress.class);
        verify(repository).save(captor.capture());
        StudentGraduationProgress saved = captor.getValue();
        assertThat(saved.getStudent()).isEqualTo(student);
        assertThat(saved.getLanguageCertFulfilled()).isTrue();
        assertThat(saved.getCheckedAt()).isNull();
        verify(academicCache).deleteAllByStudentId(studentId);
    }

    @Test
    @DisplayName("외국어 인증 row가 있으면 외국어 인증 값만 갱신한다")
    void syncLanguageCertUpdatesExistingProgressOnlyForLanguageCert() {
        UUID studentId = UUID.randomUUID();
        StudentGraduationProgress existing =
                StudentGraduationProgress.createForLanguageCert(student, false);
        ReflectionTestUtils.setField(existing, "checkedAt", java.time.Instant.parse("2026-05-01T00:00:00Z"));
        ReflectionTestUtils.setField(existing, "gpaFulfilled", Boolean.TRUE);
        StudentGraduationProgressService service = new StudentGraduationProgressService(repository, academicCache);

        when(student.getId()).thenReturn(studentId);
        when(repository.findByStudentId(studentId)).thenReturn(Optional.of(existing));

        service.syncLanguageCert(student, true);

        assertThat(existing.getLanguageCertFulfilled()).isTrue();
        assertThat(existing.getCheckedAt()).isEqualTo(java.time.Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(existing.getGpaFulfilled()).isTrue();
        verify(repository).save(existing);
        verify(academicCache).deleteAllByStudentId(studentId);
    }

    @Test
    @DisplayName("외국어 인증 값이 없으면 저장하지 않고 기존 캐시도 유지한다")
    void syncLanguageCertSkipsWhenValueIsNull() {
        StudentGraduationProgressService service = new StudentGraduationProgressService(repository, academicCache);

        service.syncLanguageCert(student, null);

        org.mockito.Mockito.verifyNoInteractions(repository, academicCache);
    }
}
