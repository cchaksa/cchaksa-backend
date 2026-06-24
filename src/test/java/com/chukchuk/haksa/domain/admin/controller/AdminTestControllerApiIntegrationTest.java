// dev 테스트 어드민 API 컨트롤러 동작을 검증하는 테스트
package com.chukchuk.haksa.domain.admin.controller;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.admin.service.AdminTestAccountService;
import com.chukchuk.haksa.domain.admin.service.AdminTestMutationService;
import com.chukchuk.haksa.domain.admin.service.AdminTestOptionService;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AdminTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTestControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTestAccountService accountService;

    @MockBean
    private AdminTestOptionService optionService;

    @MockBean
    private AdminTestMutationService mutationService;

    @Test
    @DisplayName("테스트 계정 생성 성공 시 토큰과 테스트 식별자를 반환한다")
    void createTestUser_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        AdminTestDto.TestUserResponse response = new AdminTestDto.TestUserResponse(
                userId,
                studentId,
                "test_202606231430@example.com",
                "test_202606231430",
                "access-token",
                "refresh-token"
        );
        when(accountService.createTestUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/test-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "프론트테스트",
                                  "departmentId": 1,
                                  "majorId": 1,
                                  "admissionYear": 2024
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.email").value("test_202606231430@example.com"))
                .andExpect(jsonPath("$.data.studentCode").value("test_202606231430"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("테스트 옵션 조회 성공 시 학과와 졸업요건 영역 목록을 반환한다")
    void getTestOptions_success() throws Exception {
        AdminTestDto.TestOptionsResponse response = new AdminTestDto.TestOptionsResponse(
                List.of(new AdminTestDto.DepartmentOption(1L, "CSE", "컴퓨터학과")),
                List.of(new AdminTestDto.GraduationAreaOption("전핵", "전핵"))
        );
        when(optionService.getTestOptions()).thenReturn(response);

        mockMvc.perform(get("/api/admin/test-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departments[0].id").value(1))
                .andExpect(jsonPath("$.data.departments[0].name").value("컴퓨터학과"))
                .andExpect(jsonPath("$.data.graduationAreas[0].code").value("전핵"));
    }

    @Test
    @DisplayName("학과 공개 검색 성공 시 검색 결과를 반환한다")
    void searchDepartments_success() throws Exception {
        when(optionService.searchDepartments("컴퓨터"))
                .thenReturn(List.of(new AdminTestDto.DepartmentOption(1L, "CSE", "컴퓨터학과")));

        mockMvc.perform(get("/api/admin/departments")
                        .param("keyword", "컴퓨터"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("CSE"))
                .andExpect(jsonPath("$.data[0].name").value("컴퓨터학과"));
    }

    @Test
    @DisplayName("강의 후보 조회 성공 시 검색 결과를 반환한다")
    void searchCourseOfferings_success() throws Exception {
        AdminTestDto.CourseOfferingOption option = new AdminTestDto.CourseOfferingOption(
                10L,
                "CSE101",
                "자료구조",
                2024,
                10,
                3,
                FacultyDivision.전핵,
                null,
                "컴퓨터학과"
        );
        when(optionService.searchCourseOfferings(any())).thenReturn(List.of(option));

        mockMvc.perform(get("/api/admin/course-offerings")
                        .param("keyword", "자료")
                        .param("area", "전핵")
                        .param("year", "2024")
                        .param("semester", "10")
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].offeringId").value(10))
                .andExpect(jsonPath("$.data[0].courseCode").value("CSE101"))
                .andExpect(jsonPath("$.data[0].courseName").value("자료구조"))
                .andExpect(jsonPath("$.data[0].area").value("전핵"));
    }

    @Test
    @DisplayName("현재 인증 계정 강의 데이터 수정 성공 시 성공 메시지를 반환한다")
    void updateGraduationCourses_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        doNothing().when(mutationService).updateGraduationCourses(eq(userId), any());

        mockMvc.perform(patch("/api/admin/me/graduation-courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "area": "전핵",
                                  "addOfferingIds": [10],
                                  "removeStudentCourseIds": [20],
                                  "grade": "A+",
                                  "points": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("강의 데이터가 수정되었습니다."));
    }

    @Test
    @DisplayName("현재 인증 계정 전공 상태 수정 성공 시 성공 메시지를 반환한다")
    void updateMajor_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        doNothing().when(mutationService).updateMajor(eq(userId), any());

        mockMvc.perform(patch("/api/admin/me/major")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "majorDepartmentId": 1,
                                  "dualMajorEnabled": true,
                                  "secondaryMajorDepartmentId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("전공 상태가 수정되었습니다."));
    }
}
