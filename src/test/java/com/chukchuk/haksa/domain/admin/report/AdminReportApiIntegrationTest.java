package com.chukchuk.haksa.domain.admin.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import com.chukchuk.haksa.domain.admin.auth.model.AdminStatus;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminAccountRepository;
import com.chukchuk.haksa.domain.admin.auth.security.AdminPrincipal;
import com.chukchuk.haksa.domain.admin.report.service.AdminReportService;
import com.chukchuk.haksa.domain.report.model.GraduationRequirementSnapshotStatus;
import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot;
import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReportApiIntegrationTest {
  private static final String DATABASE_NAME = "admin-report-" + UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private ReportRepository reportRepository;
  @Autowired private AdminAccountRepository accountRepository;
  @Autowired private AdminReportService adminReportService;

  private AdminAccount firstAdmin;
  private AdminAccount secondAdmin;
  private ExecutorService executor;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:"
                + DATABASE_NAME
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,SEMESTER");
  }

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();
    accountRepository.deleteAll();
    firstAdmin =
        accountRepository.save(
            new AdminAccount(
                UUID.randomUUID(),
                "admin-1",
                "첫 관리자",
                AdminRole.CS_AGENT,
                AdminStatus.ACTIVE,
                "bootstrap"));
    secondAdmin =
        accountRepository.save(
            new AdminAccount(
                UUID.randomUUID(),
                "admin-2",
                "둘째 관리자",
                AdminRole.ADMIN,
                AdminStatus.ACTIVE,
                "bootstrap"));
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void listUsesExactFiltersAndDoesNotExposeSensitiveDetailFields() throws Exception {
    UUID selectedUser = UUID.randomUUID();
    Report selected = saveReport(selectedUser, "선택 문의", "민감 본문", "20201234");
    saveReport(UUID.randomUUID(), "다른 문의", "다른 본문", "20209999");

    mockMvc
        .perform(
            get("/api/admin/reports")
                .with(authentication(authenticationOf(firstAdmin)))
                .param("status", "PENDING")
                .param("searchType", "USER_ID")
                .param("query", selectedUser.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].reportId").value(selected.getId().toString()))
        .andExpect(jsonPath("$.data.items[0].studentCode").value("20201234"))
        .andExpect(jsonPath("$.data.items[0].content").doesNotExist())
        .andExpect(jsonPath("$.data.items[0].answer").doesNotExist())
        .andExpect(jsonPath("$.data.items[0].submitter").doesNotExist());

    mockMvc
        .perform(
            get("/api/admin/reports")
                .with(authentication(authenticationOf(firstAdmin)))
                .param("searchType", "STUDENT_CODE")
                .param("query", "2020"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void detailUsesServerSnapshotAndDoesNotInventUiOnlyFields() throws Exception {
    Report report = saveReport(UUID.randomUUID(), "상세 문의", "문의 본문", "20201234");

    mockMvc
        .perform(
            get("/api/admin/reports/{reportId}", report.getId())
                .with(authentication(authenticationOf(firstAdmin))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reportId").value(report.getId().toString()))
        .andExpect(jsonPath("$.data.content").value("문의 본문"))
        .andExpect(jsonPath("$.data.submitter.departmentName").value("컴퓨터공학과"))
        .andExpect(jsonPath("$.data.submitter.primaryMajorName").value("컴퓨터공학"))
        .andExpect(jsonPath("$.data.submitter.transferStudent").value(false))
        .andExpect(jsonPath("$.data.category").doesNotExist())
        .andExpect(jsonPath("$.data.errorCode").doesNotExist())
        .andExpect(jsonPath("$.data.universityName").doesNotExist())
        .andExpect(jsonPath("$.data.grade").doesNotExist())
        .andExpect(jsonPath("$.data.semester").doesNotExist());
  }

  @Test
  void detailAllowsAnonymizedNullableSnapshot() throws Exception {
    Report report = saveReport(UUID.randomUUID(), "익명 문의", "문의 본문", "20201234");
    report.anonymizeSubmitterSnapshot();
    reportRepository.saveAndFlush(report);

    mockMvc
        .perform(
            get("/api/admin/reports/{reportId}", report.getId())
                .with(authentication(authenticationOf(firstAdmin))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.submitter.submittedUserId").isEmpty())
        .andExpect(jsonPath("$.data.submitter.studentCode").isEmpty())
        .andExpect(jsonPath("$.data.submitter.graduationRequirementStatus").value("UNKNOWN"));
  }

  @Test
  void invalidPagingAndIncompleteSearchAreRejected() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/reports")
                .with(authentication(authenticationOf(firstAdmin)))
                .param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
    mockMvc
        .perform(
            get("/api/admin/reports")
                .with(authentication(authenticationOf(firstAdmin)))
                .param("searchType", "USER_ID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
  }

  @Test
  void concurrentAnswersPersistExactlyOneAuthenticatedAdmin() throws Exception {
    Report report = saveReport(UUID.randomUUID(), "동시 답변", "문의 본문", "20201234");
    CountDownLatch start = new CountDownLatch(1);
    Future<Attempt> first = executor.submit(() -> answerAfter(start, report.getId(), firstAdmin));
    Future<Attempt> second = executor.submit(() -> answerAfter(start, report.getId(), secondAdmin));

    start.countDown();
    List<Attempt> attempts = List.of(await(first), await(second));
    assertThat(attempts).filteredOn(Attempt::success).hasSize(1);
    assertThat(attempts)
        .filteredOn(attempt -> !attempt.success())
        .extracting(Attempt::errorCode)
        .containsExactly(ErrorCode.REPORT_ALREADY_ANSWERED);

    Report answered = reportRepository.findById(report.getId()).orElseThrow();
    assertThat(answered.getStatus()).isEqualTo(ReportStatus.ANSWERED);
    assertThat(answered.getAnswer()).isIn("첫 답변", "둘째 답변");
    assertThat(answered.getAnsweredByAdmin().getId()).isIn(firstAdmin.getId(), secondAdmin.getId());
  }

  @Test
  void answerEndpointUsesPrincipalAndRejectsDuplicateAnswer() throws Exception {
    Report report = saveReport(UUID.randomUUID(), "답변 문의", "문의 본문", "20201234");

    mockMvc
        .perform(
            post("/api/admin/reports/{reportId}/answer", report.getId())
                .with(authentication(authenticationOf(firstAdmin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"  답변 내용  \"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.adminAccountId").value(firstAdmin.getId().toString()))
        .andExpect(jsonPath("$.data.status").value("ANSWERED"));

    mockMvc
        .perform(
            post("/api/admin/reports/{reportId}/answer", report.getId())
                .with(authentication(authenticationOf(secondAdmin)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"다른 답변\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("R02"));
    assertThat(reportRepository.findById(report.getId()).orElseThrow().getAnswer())
        .isEqualTo("답변 내용");
  }

  @Test
  void openApiPublishesAdminReportContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/admin/reports'].get").exists())
        .andExpect(jsonPath("$.paths['/api/admin/reports/{reportId}'].get").exists())
        .andExpect(jsonPath("$.paths['/api/admin/reports/{reportId}/answer'].post").exists());
  }

  private Attempt answerAfter(CountDownLatch start, UUID reportId, AdminAccount account)
      throws InterruptedException {
    start.await();
    try {
      adminReportService.answer(
          reportId, account == firstAdmin ? "첫 답변" : "둘째 답변", principalOf(account));
      return new Attempt(true, null);
    } catch (CommonException exception) {
      return new Attempt(false, exception.getErrorCode());
    }
  }

  private Attempt await(Future<Attempt> future) throws InterruptedException, ExecutionException {
    return future.get();
  }

  private Report saveReport(UUID userId, String title, String content, String studentCode) {
    Report report =
        Report.create(
            userId,
            title,
            content,
            new ReportSubmitterSnapshot(
                userId,
                1L,
                "컴퓨터공학과",
                studentCode,
                2L,
                "컴퓨터공학",
                null,
                null,
                false,
                2020,
                GraduationRequirementSnapshotStatus.AVAILABLE));
    return reportRepository.saveAndFlush(report);
  }

  private UsernamePasswordAuthenticationToken authenticationOf(AdminAccount account) {
    AdminPrincipal principal = principalOf(account);
    return new UsernamePasswordAuthenticationToken(
        principal,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_" + account.getAdminRole().name())));
  }

  private AdminPrincipal principalOf(AdminAccount account) {
    return new AdminPrincipal(
        account.getId(), UUID.randomUUID(), account.getDisplayName(), account.getAdminRole());
  }

  private record Attempt(boolean success, ErrorCode errorCode) {}
}
