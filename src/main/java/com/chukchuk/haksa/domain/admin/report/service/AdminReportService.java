package com.chukchuk.haksa.domain.admin.report.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.security.AdminPrincipal;
import com.chukchuk.haksa.domain.admin.report.dto.AdminReportDto;
import com.chukchuk.haksa.domain.admin.report.dto.AdminReportSearchType;
import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot;
import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 권한으로 문의를 조회하고 최초 답변을 원자적으로 저장한다. */
@Service
@RequiredArgsConstructor
public class AdminReportService {
  private final ReportRepository reportRepository;
  private final Clock clock;

  /**
   * 상태와 정확 일치 검색 조건으로 문의를 최신순 조회한다.
   *
   * @param page 0부터 시작하는 페이지
   * @param size 페이지 크기
   * @param status 선택 상태
   * @param searchType 정확 일치 검색 필드
   * @param query 검색 값
   * @return 민감 본문과 전체 스냅샷을 제외한 문의 페이지
   */
  @Transactional(readOnly = true)
  public AdminReportDto.PageResponse getReports(
      int page, int size, ReportStatus status, AdminReportSearchType searchType, String query) {
    Specification<Report> specification = (root, ignored, cb) -> cb.conjunction();
    if (status != null) {
      specification =
          specification.and((root, ignored, cb) -> cb.equal(root.get("status"), status));
    }
    Specification<Report> search = searchSpecification(searchType, query);
    if (search != null) {
      specification = specification.and(search);
    }

    Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    Page<Report> reports =
        reportRepository.findAll(specification, PageRequest.of(page, size, sort));
    return new AdminReportDto.PageResponse(
        reports.getContent().stream().map(this::toListItem).toList(),
        reports.getNumber(),
        reports.getSize(),
        reports.getTotalElements(),
        reports.getTotalPages(),
        reports.hasNext());
  }

  /**
   * 문의 본문, 실제 제출자 스냅샷과 답변 감사 정보를 조회한다.
   *
   * @param reportId 문의 UUID
   * @return 관리자 문의 상세
   * @throws EntityNotFoundException 문의가 없는 경우
   */
  @Transactional(readOnly = true)
  public AdminReportDto.DetailResponse getDetail(UUID reportId) {
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REPORT_NOT_FOUND));
    return toDetail(report);
  }

  /**
   * 답변 대기 문의에 인증 관리자의 최초 답변을 저장한다.
   *
   * @param reportId 문의 UUID
   * @param answer 정규화된 답변
   * @param principal 인증 관리자
   * @return 저장된 답변의 감사 정보
   * @throws EntityNotFoundException 문의가 없는 경우
   * @throws CommonException 문의가 이미 답변된 경우
   */
  @Transactional
  public AdminReportDto.AnswerResponse answer(
      UUID reportId, String answer, AdminPrincipal principal) {
    Instant answeredAt = clock.instant();
    int updated =
        reportRepository.answerIfPending(reportId, answer, answeredAt, principal.adminAccountId());
    if (updated == 0) {
      if (!reportRepository.existsById(reportId)) {
        throw new EntityNotFoundException(ErrorCode.REPORT_NOT_FOUND);
      }
      throw new CommonException(ErrorCode.REPORT_ALREADY_ANSWERED);
    }
    return new AdminReportDto.AnswerResponse(
        reportId,
        ReportStatus.ANSWERED,
        answeredAt,
        principal.adminAccountId(),
        principal.displayName(),
        principal.role());
  }

  private Specification<Report> searchSpecification(
      AdminReportSearchType searchType, String query) {
    if (searchType == null && (query == null || query.isBlank())) {
      return null;
    }
    if (searchType == null || query == null || query.isBlank()) {
      throw new IllegalArgumentException("searchType과 query는 함께 입력해야 합니다.");
    }
    String normalized = query.trim();
    if (searchType == AdminReportSearchType.USER_ID) {
      UUID userId;
      try {
        userId = UUID.fromString(normalized);
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("USER_ID 검색 값은 UUID여야 합니다.", exception);
      }
      UUID exactUserId = userId;
      return (root, ignored, cb) -> cb.equal(root.get("userId"), exactUserId);
    }
    if (normalized.length() > 255) {
      throw new IllegalArgumentException("STUDENT_CODE 검색 값이 너무 깁니다.");
    }
    return (root, ignored, cb) ->
        cb.equal(root.get("submitterSnapshot").get("studentCode"), normalized);
  }

  private AdminReportDto.ListItem toListItem(Report report) {
    return new AdminReportDto.ListItem(
        report.getId(),
        report.getStatus(),
        report.getTitle(),
        report.getUserId(),
        report.getSubmitterSnapshot().getStudentCode(),
        report.getCreatedAt(),
        report.getAnsweredAt());
  }

  private AdminReportDto.DetailResponse toDetail(Report report) {
    ReportSubmitterSnapshot snapshot = report.getSubmitterSnapshot();
    AdminAccount answeredBy = report.getAnsweredByAdmin();
    AdminReportDto.AnswerInfo answer =
        report.getAnswer() == null
            ? null
            : new AdminReportDto.AnswerInfo(
                report.getAnswer(),
                report.getAnsweredAt(),
                answeredBy == null ? null : answeredBy.getId(),
                answeredBy == null ? null : answeredBy.getDisplayName());
    return new AdminReportDto.DetailResponse(
        report.getId(),
        report.getStatus(),
        report.getTitle(),
        report.getContent(),
        report.getUserId(),
        report.getCreatedAt(),
        report.getUpdatedAt(),
        new AdminReportDto.SubmitterSnapshot(
            snapshot.getSubmittedUserId(),
            snapshot.getDepartmentId(),
            snapshot.getDepartmentName(),
            snapshot.getStudentCode(),
            snapshot.getPrimaryMajorId(),
            snapshot.getPrimaryMajorName(),
            snapshot.getSecondaryMajorId(),
            snapshot.getSecondaryMajorName(),
            snapshot.getTransferStudent(),
            snapshot.getAdmissionYear(),
            snapshot.getGraduationRequirementStatus()),
        answer);
  }
}
