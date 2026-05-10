package com.chukchuk.haksa.domain.student.controller;

import com.chukchuk.haksa.domain.student.dto.StudentDto;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Test
    @DisplayName("student profile 조회 성공 시 성공 응답을 반환한다")
    void getProfile_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);
        when(studentService.getStudentProfileByUserId(userId)).thenReturn(
                new StudentDto.StudentProfileResponse(
                        "홍길동",
                        "20201234",
                        "컴퓨터학과",
                        "컴퓨터학과",
                        "",
                        3,
                        1,
                        StudentStatus.재학,
                        "2026-02-22T00:00:00Z",
                        "2026-02-22T00:00:00Z",
                        false
                )
        );

        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentCode").value("20201234"));
    }

    @Test
    @DisplayName("student profile 조회 시 학생 미연결 사용자는 U04 응답을 반환한다")
    void getProfile_notConnected() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(studentService.getStudentProfileByUserId(userId))
                .thenThrow(new CommonException(ErrorCode.USER_NOT_CONNECTED));

        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("U04"));
    }

    @Test
    @DisplayName("student profile 조회 시 사용자가 없으면 U01 응답을 반환한다")
    void getProfile_userNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(studentService.getStudentProfileByUserId(userId))
                .thenThrow(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("U01"));
    }

    @Test
    @DisplayName("target-gpa 입력이 범위를 벗어나면 C01 예외를 반환한다")
    void setTargetGpa_invalid() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);
        when(studentService.getRequiredStudentIdByUserId(userId)).thenReturn(studentId);

        mockMvc.perform(post("/api/student/target-gpa")
                        .param("targetGpa", "5.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C01"));
    }
}
