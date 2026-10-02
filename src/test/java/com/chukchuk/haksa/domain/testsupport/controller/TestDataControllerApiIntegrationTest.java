// dev 테스트 어드민 API 컨트롤러 동작을 검증하는 테스트

package com.chukchuk.haksa.domain.testsupport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.testsupport.dto.TestDataDto;
import com.chukchuk.haksa.domain.testsupport.service.TestAccountService;
import com.chukchuk.haksa.domain.testsupport.service.TestLectureEvaluationService;
import com.chukchuk.haksa.domain.testsupport.service.TestMutationService;
import com.chukchuk.haksa.domain.testsupport.service.TestOptionService;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(TestDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class TestDataControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private TestAccountService accountService;

  @MockBean private TestOptionService optionService;

  @MockBean private TestMutationService mutationService;

  @MockBean private TestLectureEvaluationService lectureEvaluationService;

  @Test
  @DisplayName("테스트 계정 생성 성공 시 토큰과 테스트 식별자를 반환한다")
  void createTestUserSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    TestDataDto.TestUserResponse response =
        new TestDataDto.TestUserResponse(
            userId,
            studentId,
            "test_202606231430@example.com",
            "test_202606231430",
            "access-token",
            "refresh-token");
    when(accountService.createTestUser(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/test/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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
  void getTestOptionsSuccess() throws Exception {
    TestDataDto.TestOptionsResponse response =
        new TestDataDto.TestOptionsResponse(
            List.of(new TestDataDto.DepartmentOption(1L, "CSE", "컴퓨터학과")),
            List.of(new TestDataDto.GraduationAreaOption("전핵", "전핵")));
    when(optionService.getTestOptions()).thenReturn(response);

    mockMvc
        .perform(get("/api/test/options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.departments[0].id").value(1))
        .andExpect(jsonPath("$.data.departments[0].name").value("컴퓨터학과"))
        .andExpect(jsonPath("$.data.graduationAreas[0].code").value("전핵"));
  }

  @Test
  @DisplayName("학과 공개 검색 성공 시 검색 결과를 반환한다")
  void searchDepartmentsSuccess() throws Exception {
    when(optionService.searchDepartments("컴퓨터"))
        .thenReturn(List.of(new TestDataDto.DepartmentOption(1L, "CSE", "컴퓨터학과")));

    mockMvc
        .perform(get("/api/test/departments").param("keyword", "컴퓨터"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].code").value("CSE"))
        .andExpect(jsonPath("$.data[0].name").value("컴퓨터학과"));
  }

  @Test
  @DisplayName("강의 후보 조회 성공 시 검색 결과를 반환한다")
  void searchCourseOfferingsSuccess() throws Exception {
    TestDataDto.CourseOfferingOption option =
        new TestDataDto.CourseOfferingOption(
            10L, "CSE101", "자료구조", 2024, 10, 3, FacultyDivision.전핵, null, "컴퓨터학과");
    when(optionService.searchCourseOfferings(any())).thenReturn(List.of(option));

    mockMvc
        .perform(
            get("/api/test/course-offerings")
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
  void updateGraduationCoursesSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    doNothing().when(mutationService).updateGraduationCourses(eq(userId), any());

    mockMvc
        .perform(
            patch("/api/test/me/graduation-courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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
  void updateMajorSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    doNothing().when(mutationService).updateMajor(eq(userId), any());

    mockMvc
        .perform(
            patch("/api/test/me/major")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
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

  @Test
  @DisplayName("현재 인증 계정 초기화 성공 시 성공 메시지를 반환한다")
  void resetCurrentAccountSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    doNothing().when(mutationService).resetCurrentAccount(userId);

    mockMvc
        .perform(post("/api/test/me/reset"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("테스트 데이터가 초기화되었습니다."));
  }

  @Test
  @DisplayName("현재 인증 계정 테스트 강의 생성 성공 시 생성 결과를 반환한다")
  void createTestCourseSuccess() throws Exception {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    TestDataDto.TestCourseResponse response =
        new TestDataDto.TestCourseResponse(
            40L, 30L, "test_CSE101", "프론트 테스트 강의", FacultyDivision.전선);
    when(mutationService.createTestCourse(eq(userId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/test/me/test-courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "courseCode": "CSE101",
                                  "courseName": "프론트 테스트 강의",
                                  "area": "전선",
                                  "departmentId": 1,
                                  "year": 2026,
                                  "semester": 10,
                                  "credits": 3,
                                  "grade": "A+"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.studentCourseId").value(40))
        .andExpect(jsonPath("$.data.offeringId").value(30))
        .andExpect(jsonPath("$.data.courseCode").value("test_CSE101"))
        .andExpect(jsonPath("$.data.courseName").value("프론트 테스트 강의"))
        .andExpect(jsonPath("$.data.area").value("전선"));
  }

  @Test
  @DisplayName("강의평가 empty-semester 테스트 상태 세팅 성공 시 성공 메시지를 반환한다")
  void setLectureEvaluationEmptySemesterSuccess() throws Exception {
    mockMvc
        .perform(post("/api/test/lecture-evaluations/empty-semester"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("강의평가 테스트 상태가 empty-semester로 변경되었습니다."));

    verify(lectureEvaluationService).setEmptySemester();
  }

  @Test
  @DisplayName("강의평가 NOT_RELEASED 테스트 상태 세팅 성공 시 성공 메시지를 반환한다")
  void setLectureEvaluationNotReleasedSuccess() throws Exception {
    mockMvc
        .perform(post("/api/test/lecture-evaluations/not-released"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("강의평가 테스트 상태가 NOT_RELEASED로 변경되었습니다."));

    verify(lectureEvaluationService).setNotReleased();
  }

  @Test
  @DisplayName("강의평가 PENDING 테스트 상태 세팅 성공 시 성공 메시지를 반환한다")
  void setLectureEvaluationPendingSuccess() throws Exception {
    mockMvc
        .perform(post("/api/test/lecture-evaluations/pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("강의평가 테스트 상태가 PENDING으로 변경되었습니다."));

    verify(lectureEvaluationService).setPending();
  }

  @Test
  @DisplayName("강의평가 SKIPPED 테스트 상태 세팅 성공 시 성공 메시지를 반환한다")
  void setLectureEvaluationSkippedSuccess() throws Exception {
    mockMvc
        .perform(post("/api/test/lecture-evaluations/skipped"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("강의평가 테스트 상태가 SKIPPED로 변경되었습니다."));

    verify(lectureEvaluationService).setSkipped();
  }

  @Test
  @DisplayName("강의평가 COMPLETED 테스트 상태 세팅 성공 시 성공 메시지를 반환한다")
  void setLectureEvaluationCompletedSuccess() throws Exception {
    mockMvc
        .perform(post("/api/test/lecture-evaluations/completed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.message").value("강의평가 테스트 상태가 COMPLETED로 변경되었습니다."));

    verify(lectureEvaluationService).setCompleted();
  }
}
