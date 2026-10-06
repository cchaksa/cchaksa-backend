package com.chukchuk.haksa.domain.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.domain.report.service.ReportService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

  @Autowired private MockMvc mockMvc;
  @MockBean private ReportService reportService;

  @Test
  @DisplayName("문의 등록은 201과 상세 Location을 반환한다")
  void createReturnsCreated() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    authenticate(userId);
    when(reportService.create(eq(userId), any()))
        .thenReturn(
            new ReportDto.CreateResponse(
                reportId, ReportStatus.PENDING, Instant.parse("2026-09-22T00:00:00Z")));

    mockMvc
        .perform(
            post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"문의 제목\",\"content\":\"문의 본문\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/reports/" + reportId))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  @DisplayName("문의 등록 요청의 사용자 스냅샷 필드는 서비스 입력에 반영되지 않는다")
  void createDoesNotBindSnapshotFields() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    authenticate(userId);
    when(reportService.create(
            eq(userId),
            argThat(
                request -> "문의 제목".equals(request.title()) && "문의 본문".equals(request.content()))))
        .thenReturn(new ReportDto.CreateResponse(reportId, ReportStatus.PENDING, Instant.now()));

    mockMvc
        .perform(
            post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"문의 제목","content":"문의 본문","studentCode":"조작값"}
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("빈 문의 제목은 C01로 거부한다")
  void createRejectsBlankTitle() throws Exception {
    authenticate(UUID.randomUUID());

    mockMvc
        .perform(
            post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"content\":\"본문\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
  }

  @Test
  @DisplayName("내 문의 목록을 페이지 정보와 함께 반환한다")
  void getMyReportsReturnsPage() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    authenticate(userId);
    when(reportService.getMyReports(userId, 0, 20))
        .thenReturn(
            new ReportDto.PageResponse(
                List.of(
                    new ReportDto.ListItem(
                        reportId,
                        ReportStatus.PENDING,
                        "문의 제목",
                        Instant.parse("2026-09-22T00:00:00Z"),
                        null)),
                0,
                20,
                1,
                1,
                false));

    mockMvc
        .perform(get("/api/reports"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].id").value(reportId.toString()))
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  @DisplayName("허용 범위를 벗어난 페이지 크기는 C01로 거부한다")
  void getMyReportsRejectsInvalidPageSize() throws Exception {
    authenticate(UUID.randomUUID());

    mockMvc
        .perform(get("/api/reports").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
  }

  @Test
  @DisplayName("다른 사용자의 문의 상세 조회는 403 C04를 반환한다")
  void getDetailReturnsForbidden() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    authenticate(userId);
    when(reportService.getDetail(userId, reportId))
        .thenThrow(new CommonException(ErrorCode.FORBIDDEN));

    mockMvc
        .perform(get("/api/reports/{reportId}", reportId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("C04"))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.title").doesNotExist());
  }

  @Test
  @DisplayName("존재하지 않는 문의 상세 조회는 404 R01을 반환한다")
  void getDetailReturnsNotFound() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    authenticate(userId);
    when(reportService.getDetail(userId, reportId))
        .thenThrow(new EntityNotFoundException(ErrorCode.REPORT_NOT_FOUND));

    mockMvc
        .perform(get("/api/reports/{reportId}", reportId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("R01"));
  }
}
