package com.chukchuk.haksa.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.graduation.policy.MajorResolutionResult;
import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.GraduationRequirementSnapshotStatus;
import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceUnitTests {

  @Mock private ReportRepository reportRepository;
  @Mock private UserRepository userRepository;
  @Mock private GraduationMajorResolver graduationMajorResolver;
  @InjectMocks private ReportService reportService;

  @Test
  @DisplayName("문의 생성 시 사용자와 학적 정보를 스냅샷으로 저장한다")
  void createStoresSubmitterSnapshot() {
    final UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    Student student = mock(Student.class);
    Department department = new Department("D1", "컴퓨터학과");
    Department secondaryMajor = new Department("D2", "경영학과");
    ReflectionTestUtils.setField(department, "id", 1L);
    ReflectionTestUtils.setField(secondaryMajor, "id", 2L);
    AcademicInfo academicInfo =
        AcademicInfo.builder().admissionYear(2022).isTransferStudent(true).build();

    when(user.getStudent()).thenReturn(student);
    when(student.getDepartment()).thenReturn(department);
    when(student.getMajor()).thenReturn(null);
    when(student.getSecondaryMajor()).thenReturn(secondaryMajor);
    when(student.getAcademicInfo()).thenReturn(academicInfo);
    when(student.getStudentCode()).thenReturn("20221234");
    when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));
    when(graduationMajorResolver.resolve(student, 2022))
        .thenReturn(new MajorResolutionResult(1L, 2L));
    when(reportRepository.saveAndFlush(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report report = invocation.getArgument(0);
              ReflectionTestUtils.setField(report, "id", UUID.randomUUID());
              ReflectionTestUtils.setField(report, "createdAt", java.time.Instant.now());
              return report;
            });

    ReportDto.CreateResponse response =
        reportService.create(userId, new ReportDto.CreateRequest(" 제목 ", " 본문 "));

    ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
    verify(reportRepository).saveAndFlush(captor.capture());
    Report saved = captor.getValue();
    assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    assertThat(saved.getTitle()).isEqualTo("제목");
    assertThat(saved.getContent()).isEqualTo("본문");
    assertThat(saved.getSubmitterSnapshot().getSubmittedUserId()).isEqualTo(userId);
    assertThat(saved.getSubmitterSnapshot().getDepartmentName()).isEqualTo("컴퓨터학과");
    assertThat(saved.getSubmitterSnapshot().getPrimaryMajorId()).isEqualTo(1L);
    assertThat(saved.getSubmitterSnapshot().getSecondaryMajorName()).isEqualTo("경영학과");
    assertThat(saved.getSubmitterSnapshot().getTransferStudent()).isTrue();
    assertThat(saved.getSubmitterSnapshot().getGraduationRequirementStatus())
        .isEqualTo(GraduationRequirementSnapshotStatus.AVAILABLE);
  }

  @Test
  @DisplayName("졸업요건을 찾을 수 없으면 문의는 NOT_AVAILABLE 스냅샷으로 생성한다")
  void createStoresNotAvailableWhenGraduationRequirementDoesNotExist() {
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    Student student = mock(Student.class);
    Department department = new Department("D1", "컴퓨터학과");
    AcademicInfo academicInfo = AcademicInfo.builder().admissionYear(2022).build();
    when(user.getStudent()).thenReturn(student);
    when(student.getDepartment()).thenReturn(department);
    when(student.getAcademicInfo()).thenReturn(academicInfo);
    when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));
    when(graduationMajorResolver.resolve(student, 2022))
        .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));
    when(reportRepository.saveAndFlush(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report report = invocation.getArgument(0);
              ReflectionTestUtils.setField(report, "id", UUID.randomUUID());
              ReflectionTestUtils.setField(report, "createdAt", java.time.Instant.now());
              return report;
            });

    reportService.create(userId, new ReportDto.CreateRequest("제목", "본문"));

    ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
    verify(reportRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getSubmitterSnapshot().getGraduationRequirementStatus())
        .isEqualTo(GraduationRequirementSnapshotStatus.NOT_AVAILABLE);
  }

  @Test
  @DisplayName("학생 정보가 없으면 UNKNOWN 스냅샷으로 문의를 생성한다")
  void createStoresUnknownWithoutStudent() {
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));
    when(reportRepository.saveAndFlush(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report report = invocation.getArgument(0);
              ReflectionTestUtils.setField(report, "id", UUID.randomUUID());
              ReflectionTestUtils.setField(report, "createdAt", java.time.Instant.now());
              return report;
            });

    reportService.create(userId, new ReportDto.CreateRequest("제목", "본문"));

    ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
    verify(reportRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getSubmitterSnapshot().getSubmittedUserId()).isEqualTo(userId);
    assertThat(captor.getValue().getSubmitterSnapshot().getGraduationRequirementStatus())
        .isEqualTo(GraduationRequirementSnapshotStatus.UNKNOWN);
  }

  @Test
  @DisplayName("졸업요건 없음 이외의 resolver 예외는 문의 생성에서 전파한다")
  void createPropagatesUnexpectedResolverException() {
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    Student student = mock(Student.class);
    Department department = new Department("D1", "컴퓨터학과");
    AcademicInfo academicInfo = AcademicInfo.builder().admissionYear(2022).build();
    when(user.getStudent()).thenReturn(student);
    when(student.getDepartment()).thenReturn(department);
    when(student.getAcademicInfo()).thenReturn(academicInfo);
    when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));
    when(graduationMajorResolver.resolve(student, 2022))
        .thenThrow(new CommonException(ErrorCode.REFRESH_FAILED));

    assertThatThrownBy(() -> reportService.create(userId, new ReportDto.CreateRequest("제목", "본문")))
        .isInstanceOf(CommonException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.REFRESH_FAILED);
  }

  @Test
  @DisplayName("존재하는 다른 사용자의 문의 상세 조회는 C04로 거부한다")
  void getDetailRejectsOtherOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    Report report =
        Report.create(
            ownerId,
            "제목",
            "본문",
            com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot.unknown(ownerId));
    ReflectionTestUtils.setField(report, "id", reportId);
    when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

    assertThatThrownBy(() -> reportService.getDetail(requesterId, reportId))
        .isInstanceOf(CommonException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("존재하지 않는 문의 상세 조회는 R01을 반환한다")
  void getDetailReturnsReportNotFound() {
    UUID reportId = UUID.randomUUID();
    when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.getDetail(UUID.randomUUID(), reportId))
        .isInstanceOf(EntityNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
  }
}
