package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAcademicRecordServiceUnitTests {

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @Mock
    private GraduationQueryRepository graduationQueryRepository;

    @Mock
    private AcademicCache academicCache;

    @InjectMocks
    private StudentAcademicRecordService studentAcademicRecordService;

    @Test
    @DisplayName("학업 요약 캐시가 있으면 캐시 값을 그대로 반환한다")
    void getAcademicSummary_cacheHit_returnsCached() {
        UUID studentId = UUID.randomUUID();
        StudentAcademicRecordDto.AcademicSummaryResponse cached =
                new StudentAcademicRecordDto.AcademicSummaryResponse(
                        100, new BigDecimal("3.50"), new BigDecimal("88.1"), 130
                );
        when(academicCache.getAcademicSummary(studentId)).thenReturn(cached);

        StudentAcademicRecordDto.AcademicSummaryResponse result = studentAcademicRecordService.getAcademicSummary(studentId);

        assertThat(result).isEqualTo(cached);
        verify(studentAcademicRecordRepository, never()).findByStudentId(any(UUID.class));
    }

    @Test
    @DisplayName("단일전공인 경우 전공 요건 총합으로 requiredCredits를 계산한다")
    void getAcademicSummary_singleMajor_calculatesRequiredCredits() {
        UUID studentId = UUID.randomUUID();
        Student student = student(10L, null, 2022);
        StudentAcademicRecord record = new StudentAcademicRecord(
                student, 120, 110, new BigDecimal("3.65"), new BigDecimal("90.0")
        );

        when(academicCache.getAcademicSummary(studentId)).thenReturn(null);
        when(studentAcademicRecordRepository.findByStudentId(studentId)).thenReturn(Optional.of(record));
        when(graduationQueryRepository.getAreaRequirementsWithCache(10L, 2022))
                .thenReturn(List.of(
                        new AreaRequirementDto("전핵", 60, null, null),
                        new AreaRequirementDto("전선", 30, null, null),
                        new AreaRequirementDto("일선", 40, null, null)
                ));

        StudentAcademicRecordDto.AcademicSummaryResponse result = studentAcademicRecordService.getAcademicSummary(studentId);

        assertThat(result.requiredCredits()).isEqualTo(130);
        assertThat(result.totalEarnedCredits()).isEqualTo(110);
        verify(academicCache).setAcademicSummary(studentId, result);
    }

    @Test
    @DisplayName("복수전공인 경우 주전공(전선/일선 제외)+복수전공 합계로 requiredCredits를 계산한다")
    void getAcademicSummary_dualMajor_calculatesRequiredCredits() {
        UUID studentId = UUID.randomUUID();
        Student student = student(20L, 30L, 2021);
        StudentAcademicRecord record = new StudentAcademicRecord(
                student, 130, 120, new BigDecimal("3.40"), new BigDecimal("85.5")
        );

        when(academicCache.getAcademicSummary(studentId)).thenReturn(null);
        when(studentAcademicRecordRepository.findByStudentId(studentId)).thenReturn(Optional.of(record));
        when(graduationQueryRepository.getAreaRequirementsWithCache(20L, 2021))
                .thenReturn(List.of(
                        new AreaRequirementDto("전핵", 36, null, null),
                        new AreaRequirementDto("전선", 24, null, null),
                        new AreaRequirementDto("일선", 20, null, null)
                ));
        when(graduationQueryRepository.getDualMajorRequirementsWithCache(20L, 30L, 2021))
                .thenReturn(List.of(
                        new AreaRequirementDto("복핵", 33, null, null),
                        new AreaRequirementDto("복선", 21, null, null)
                ));

        StudentAcademicRecordDto.AcademicSummaryResponse result = studentAcademicRecordService.getAcademicSummary(studentId);

        assertThat(result.requiredCredits()).isEqualTo(130);
        assertThat(result.totalEarnedCredits()).isEqualTo(120);
    }

    @Test
    @DisplayName("학업 기록이 없으면 STUDENT_ACADEMIC_RECORD_NOT_FOUND 예외를 던진다")
    void getAcademicSummary_recordMissing_throws() {
        UUID studentId = UUID.randomUUID();
        when(academicCache.getAcademicSummary(studentId)).thenReturn(null);
        when(studentAcademicRecordRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentAcademicRecordService.getAcademicSummary(studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("캐시 set 실패가 발생해도 학업 요약 응답은 정상 반환한다")
    void getAcademicSummary_cacheSetFails_stillReturns() {
        UUID studentId = UUID.randomUUID();
        Student student = student(10L, null, 2022);
        StudentAcademicRecord record = new StudentAcademicRecord(
                student, 120, 110, new BigDecimal("3.65"), new BigDecimal("90.0")
        );

        when(academicCache.getAcademicSummary(studentId)).thenReturn(null);
        when(studentAcademicRecordRepository.findByStudentId(studentId)).thenReturn(Optional.of(record));
        when(graduationQueryRepository.getAreaRequirementsWithCache(10L, 2022))
                .thenReturn(List.of(new AreaRequirementDto("전핵", 60, null, null)));
        org.mockito.Mockito.doThrow(new RuntimeException("cache set fail"))
                .when(academicCache).setAcademicSummary(any(UUID.class), any(StudentAcademicRecordDto.AcademicSummaryResponse.class));

        StudentAcademicRecordDto.AcademicSummaryResponse result = studentAcademicRecordService.getAcademicSummary(studentId);

        assertThat(result.requiredCredits()).isEqualTo(60);
    }

    @Test
    @DisplayName("studentId로 학업 기록을 조회할 수 있다")
    void getStudentAcademicRecordByStudentId_success() {
        UUID studentId = UUID.randomUUID();
        StudentAcademicRecord record = org.mockito.Mockito.mock(StudentAcademicRecord.class);
        when(studentAcademicRecordRepository.findByStudentId(studentId)).thenReturn(Optional.of(record));

        StudentAcademicRecord found = studentAcademicRecordService.getStudentAcademicRecordByStudentId(studentId);

        assertThat(found).isSameAs(record);
    }

    private Student student(Long majorId, Long secondaryMajorId, Integer admissionYear) {
        Student student = org.mockito.Mockito.mock(Student.class);
        Department major = org.mockito.Mockito.mock(Department.class);
        Department department = org.mockito.Mockito.mock(Department.class);
        lenient().when(major.getId()).thenReturn(majorId);
        lenient().when(department.getId()).thenReturn(majorId);
        lenient().when(student.getMajor()).thenReturn(major);
        lenient().when(student.getDepartment()).thenReturn(department);
        if (secondaryMajorId != null) {
            Department secondary = org.mockito.Mockito.mock(Department.class);
            lenient().when(secondary.getId()).thenReturn(secondaryMajorId);
            lenient().when(student.getSecondaryMajor()).thenReturn(secondary);
        } else {
            lenient().when(student.getSecondaryMajor()).thenReturn(null);
        }
        lenient().when(student.getAcademicInfo()).thenReturn(
                AcademicInfo.builder()
                        .admissionYear(admissionYear)
                        .status(StudentStatus.재학)
                        .isTransferStudent(false)
                        .build()
        );
        return student;
    }
}
