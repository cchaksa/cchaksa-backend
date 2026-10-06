// 사용자 문의 생성과 본인 문의 조회를 처리한다.

package com.chukchuk.haksa.domain.report.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.GraduationRequirementSnapshotStatus;
import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot;
import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 문의 생성 시 제출자 스냅샷을 보존하고 인증 사용자의 문의만 조회한다. */
@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;
  private final UserRepository userRepository;
  private final GraduationMajorResolver graduationMajorResolver;

  /**
   * 문의와 생성 시점 사용자·학적 스냅샷을 저장한다.
   *
   * @param userId 인증 사용자 식별자
   * @param request 제목과 본문
   * @return 생성된 문의 식별자와 상태
   */
  @Transactional
  public ReportDto.CreateResponse create(UUID userId, ReportDto.CreateRequest request) {
    User user =
        userRepository
            .findProfileByIdWithAssociations(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    Report report =
        Report.create(userId, request.title(), request.content(), createSnapshot(userId, user));
    Report saved = reportRepository.saveAndFlush(report);
    return new ReportDto.CreateResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
  }

  /**
   * 인증 사용자의 문의를 최신순으로 조회한다.
   *
   * @param userId 인증 사용자 식별자
   * @param page 0부터 시작하는 페이지
   * @param size 페이지 크기
   * @return 문의 페이지
   */
  @Transactional(readOnly = true)
  public ReportDto.PageResponse getMyReports(UUID userId, int page, int size) {
    Page<Report> reports =
        reportRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
            userId, PageRequest.of(page, size));
    return new ReportDto.PageResponse(
        reports.getContent().stream().map(this::toListItem).toList(),
        reports.getNumber(),
        reports.getSize(),
        reports.getTotalElements(),
        reports.getTotalPages(),
        reports.hasNext());
  }

  /**
   * 문의 존재 여부를 먼저 확인하고 본인 소유 문의만 반환한다.
   *
   * @param userId 인증 사용자 식별자
   * @param reportId 문의 식별자
   * @return 문의 상세 내용과 관리자 답변
   * @throws CommonException 문의가 다른 사용자 소유인 경우
   */
  @Transactional(readOnly = true)
  public ReportDto.DetailResponse getDetail(UUID userId, UUID reportId) {
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REPORT_NOT_FOUND));
    if (!report.isOwnedBy(userId)) {
      throw new CommonException(ErrorCode.FORBIDDEN);
    }
    return new ReportDto.DetailResponse(
        report.getId(),
        report.getStatus(),
        report.getTitle(),
        report.getContent(),
        report.getCreatedAt(),
        report.getAnswer(),
        report.getAnsweredAt());
  }

  private ReportSubmitterSnapshot createSnapshot(UUID userId, User user) {
    Student student = user.getStudent();
    if (student == null) {
      return ReportSubmitterSnapshot.unknown(userId);
    }

    Department department = student.getDepartment();
    Department primaryMajor = student.getMajor() != null ? student.getMajor() : department;
    Department secondaryMajor = student.getSecondaryMajor();
    Integer admissionYear =
        student.getAcademicInfo() == null ? null : student.getAcademicInfo().getAdmissionYear();

    return new ReportSubmitterSnapshot(
        userId,
        idOf(department),
        nameOf(department),
        student.getStudentCode(),
        idOf(primaryMajor),
        nameOf(primaryMajor),
        idOf(secondaryMajor),
        nameOf(secondaryMajor),
        student.getAcademicInfo() == null ? null : student.getAcademicInfo().getIsTransferStudent(),
        admissionYear,
        resolveGraduationRequirementStatus(student, admissionYear));
  }

  private GraduationRequirementSnapshotStatus resolveGraduationRequirementStatus(
      Student student, Integer admissionYear) {
    if (admissionYear == null) {
      return GraduationRequirementSnapshotStatus.UNKNOWN;
    }
    try {
      graduationMajorResolver.resolve(student, admissionYear);
      return GraduationRequirementSnapshotStatus.AVAILABLE;
    } catch (CommonException exception) {
      if (exception.getErrorCode() == ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND) {
        return GraduationRequirementSnapshotStatus.NOT_AVAILABLE;
      }
      throw exception;
    }
  }

  private ReportDto.ListItem toListItem(Report report) {
    return new ReportDto.ListItem(
        report.getId(),
        report.getStatus(),
        report.getTitle(),
        report.getCreatedAt(),
        report.getAnsweredAt());
  }

  private Long idOf(Department department) {
    return department == null ? null : department.getId();
  }

  private String nameOf(Department department) {
    return department == null ? null : department.getEstablishedDepartmentName();
  }
}
