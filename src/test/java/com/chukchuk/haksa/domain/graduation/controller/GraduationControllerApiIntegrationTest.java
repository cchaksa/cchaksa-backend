package com.chukchuk.haksa.domain.graduation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.domain.graduation.dto.TransferGraduationProgressDto;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertMatchStatus;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertTestType;
import com.chukchuk.haksa.domain.graduation.service.GraduationService;
import com.chukchuk.haksa.domain.graduation.service.LanguageCertRequirementService;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraduationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GraduationControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

  @Autowired private MockMvc mockMvc;

  @MockBean private GraduationService graduationService;

  @MockBean private LanguageCertRequirementService languageCertRequirementService;

  @MockBean private StudentService studentService;

  @Test
  @DisplayName("graduation progress 조회 성공 시 성공 응답을 반환한다")
  void getGraduationProgressSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    authenticate(userId, studentId);
    when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);

    when(graduationService.getGraduationProgress(studentId))
        .thenReturn(new GraduationProgressResponse(List.of(), true));

    mockMvc
        .perform(get("/api/graduation/progress"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.analysisType").value("REGULAR"))
        .andExpect(jsonPath("$.data.analysisStatus").value("CALCULATED"))
        .andExpect(jsonPath("$.data.transferProgress").doesNotExist())
        .andExpect(jsonPath("$.data.languageCertFulfilled").value(true))
        .andExpect(jsonPath("$.data.languageCertNeedsRefresh").value(false));
  }

  @Test
  @DisplayName("편입생 졸업진단은 편입 전용 진행률과 수동 확인 상태를 반환한다")
  void getTransferGraduationProgressSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    authenticate(userId, studentId);
    when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);

    TransferGraduationProgressDto transferProgress =
        new TransferGraduationProgressDto(
            130,
            112,
            18,
            false,
            65,
            new BigDecimal("3.2"),
            new BigDecimal("2.0"),
            true,
            3,
            false,
            List.of(),
            true,
            List.of());
    when(graduationService.getGraduationProgress(studentId))
        .thenReturn(GraduationProgressResponse.forTransfer(transferProgress, true));

    mockMvc
        .perform(get("/api/graduation/progress"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.analysisType").value("TRANSFER"))
        .andExpect(jsonPath("$.data.analysisStatus").value("MANUAL_REVIEW_REQUIRED"))
        .andExpect(jsonPath("$.data.graduationProgress").isEmpty())
        .andExpect(jsonPath("$.data.transferProgress.totalEarnedCredits").value(112))
        .andExpect(jsonPath("$.data.transferProgress.designatedCourses").isEmpty())
        .andExpect(jsonPath("$.data.transferProgress.manualReviewRequired").value(true));
  }

  @Test
  @DisplayName("졸업요건 데이터가 없으면 G02 예외를 반환한다")
  void getGraduationProgressNotFound() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    authenticate(userId, studentId);
    when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);

    when(graduationService.getGraduationProgress(studentId))
        .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));

    mockMvc
        .perform(get("/api/graduation/progress"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("G02"));
  }

  @Test
  @DisplayName("외국어 인증 기준 조회 성공 시 기준 목록을 반환한다")
  void getLanguageCertRequirementSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    authenticate(userId, studentId);
    when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);
    when(languageCertRequirementService.getRequirement(studentId))
        .thenReturn(
            new LanguageCertRequirementResponse(
                "2000514",
                "컴퓨터SW",
                2021,
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                LanguageCertMatchStatus.VERIFIED,
                "컴퓨터SW 21학번 이후 기준",
                List.of(
                    new LanguageCertRequirementResponse.Requirement(
                        LanguageCertTestType.TOEIC, 600, null, "TOEIC 600점 이상", 1))));

    mockMvc
        .perform(get("/api/graduation/language-cert/requirement"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.departmentCode").value("2000514"))
        .andExpect(jsonPath("$.data.policyGroupKey").value("ICT_OTHER"))
        .andExpect(jsonPath("$.data.matchStatus").value("VERIFIED"))
        .andExpect(jsonPath("$.data.requirements[0].testType").value("TOEIC"))
        .andExpect(jsonPath("$.data.requirements[0].minimumScore").value(600));
  }

  @Test
  @DisplayName("외국어 인증 기준 미매핑도 200 응답으로 반환한다")
  void getLanguageCertRequirementUnmapped() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    authenticate(userId, studentId);
    when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);
    when(languageCertRequirementService.getRequirement(studentId))
        .thenReturn(
            LanguageCertRequirementResponse.unmapped("2000763", "자유전공학부", 2025, "기준표에 직접 행이 없음"));

    mockMvc
        .perform(get("/api/graduation/language-cert/requirement"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.departmentCode").value("2000763"))
        .andExpect(jsonPath("$.data.matchStatus").value("UNMAPPED"))
        .andExpect(jsonPath("$.data.requirements").isEmpty());
  }
}
