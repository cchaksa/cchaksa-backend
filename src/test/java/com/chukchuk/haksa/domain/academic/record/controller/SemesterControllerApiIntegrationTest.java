package com.chukchuk.haksa.domain.academic.record.controller;

import com.chukchuk.haksa.domain.academic.record.service.SemesterAcademicRecordService;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SemesterController.class)
@AutoConfigureMockMvc(addFilters = false)
class SemesterControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SemesterAcademicRecordService semesterAcademicRecordService;

    @Test
    @DisplayName("semester 목록 조회 성공 시 성공 응답을 반환한다")
    void getSemesterRecord_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(semesterAcademicRecordService.getSemestersByStudentId(studentId))
                .thenReturn(List.of(new StudentSemesterDto.StudentSemesterInfoResponse(2024, 10)));

        mockMvc.perform(get("/api/semester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].year").value(2024));
    }

    @Test
    @DisplayName("semester grades 데이터가 없으면 A02 예외를 반환한다")
    void getSemesterGrades_empty() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(semesterAcademicRecordService.getAllSemesterGrades(studentId))
                .thenThrow(new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_EMPTY));

        mockMvc.perform(get("/api/semester/grades"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A02"));
    }
}
