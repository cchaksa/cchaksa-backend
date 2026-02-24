package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterAcademicRecordServiceUnitTests {

    @Mock
    private SemesterAcademicRecordRepository semesterAcademicRecordRepository;

    @Mock
    private AcademicCache academicCache;

    @InjectMocks
    private SemesterAcademicRecordService semesterAcademicRecordService;

    @Test
    @DisplayName("특정 학기 성적 조회 성공 시 응답 DTO를 반환한다")
    void getSemesterGradesByYearAndSemester_success() {
        UUID studentId = UUID.randomUUID();
        SemesterAcademicRecord record = semesterRecord(2024, 1);
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2024, 1))
                .thenReturn(Optional.of(record));

        var result = semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, 2024, 1);

        assertThat(result.year()).isEqualTo(2024);
        assertThat(result.semester()).isEqualTo(1);
        assertThat(result.earnedCredits()).isEqualTo(15);
    }

    @Test
    @DisplayName("특정 학기 성적이 없으면 SEMESTER_RECORD_NOT_FOUND 예외를 던진다")
    void getSemesterGradesByYearAndSemester_notFound_throws() {
        UUID studentId = UUID.randomUUID();
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2024, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, 2024, 1))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.SEMESTER_RECORD_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("전체 학기 성적이 비어 있으면 SEMESTER_RECORD_EMPTY 예외를 던진다")
    void getAllSemesterGrades_empty_throws() {
        UUID studentId = UUID.randomUUID();
        when(academicCache.getSemesterSummaries(studentId)).thenReturn(List.of());

        assertThatThrownBy(() -> semesterAcademicRecordService.getAllSemesterGrades(studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.SEMESTER_RECORD_EMPTY.code()));
    }

    @Test
    @DisplayName("학기 목록 조회 시 데이터가 비어 있으면 FRESHMAN_NO_SEMESTER 예외를 던진다")
    void getSemestersByStudentId_empty_throws() {
        UUID studentId = UUID.randomUUID();
        when(academicCache.getSemesterSummaries(studentId)).thenReturn(List.of());

        assertThatThrownBy(() -> semesterAcademicRecordService.getSemestersByStudentId(studentId))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.FRESHMAN_NO_SEMESTER.code()));
    }

    @Test
    @DisplayName("캐시에 학기 요약이 있으면 저장소를 조회하지 않고 반환한다")
    void getSemesterSummaries_cacheHit_returnsCached() {
        UUID studentId = UUID.randomUUID();
        List<SemesterSummaryResponse> cached = List.of(
                new SemesterSummaryResponse(2024, 1, 15, 18, new BigDecimal("3.8"), 5, 120, new BigDecimal("92.4"))
        );
        when(academicCache.getSemesterSummaries(studentId)).thenReturn(cached);

        List<SemesterSummaryResponse> result = semesterAcademicRecordService.getSemesterSummaries(studentId);

        assertThat(result).isEqualTo(cached);
        verify(semesterAcademicRecordRepository, never()).findByStudentIdOrderByYearDescSemesterDesc(any());
    }

    @Test
    @DisplayName("캐시 조회 실패 시에도 저장소 데이터를 반환하고 캐시 저장을 시도한다")
    void getSemesterSummaries_cacheGetFails_fallbacksToRepository() {
        UUID studentId = UUID.randomUUID();
        SemesterAcademicRecord record = semesterRecord(2023, 2);
        when(academicCache.getSemesterSummaries(studentId)).thenThrow(new RuntimeException("cache get fail"));
        when(semesterAcademicRecordRepository.findByStudentIdOrderByYearDescSemesterDesc(studentId))
                .thenReturn(List.of(record));

        List<SemesterSummaryResponse> result = semesterAcademicRecordService.getSemesterSummaries(studentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).year()).isEqualTo(2023);
        verify(academicCache).setSemesterSummaries(any(UUID.class), any(List.class));
    }

    @Test
    @DisplayName("저장소 결과가 비어 있으면 캐시에 저장하지 않고 빈 리스트를 반환한다")
    void getSemesterSummaries_repositoryEmpty_returnsEmpty() {
        UUID studentId = UUID.randomUUID();
        when(academicCache.getSemesterSummaries(studentId)).thenReturn(null);
        when(semesterAcademicRecordRepository.findByStudentIdOrderByYearDescSemesterDesc(studentId))
                .thenReturn(List.of());

        List<SemesterSummaryResponse> result = semesterAcademicRecordService.getSemesterSummaries(studentId);

        assertThat(result).isEmpty();
        verify(academicCache, never()).setSemesterSummaries(any(UUID.class), any(List.class));
    }

    private SemesterAcademicRecord semesterRecord(int year, int semester) {
        Student student = org.mockito.Mockito.mock(Student.class);
        return new SemesterAcademicRecord(
                student,
                year,
                semester,
                18,
                15,
                new BigDecimal("3.80"),
                new BigDecimal("92.4"),
                new BigDecimal("3.90"),
                5,
                120
        );
    }
}
