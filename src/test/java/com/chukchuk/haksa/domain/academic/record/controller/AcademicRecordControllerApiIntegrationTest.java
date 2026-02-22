package com.chukchuk.haksa.domain.academic.record.controller;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.service.AcademicRecordService;
import com.chukchuk.haksa.domain.academic.record.service.StudentAcademicRecordService;
import com.chukchuk.haksa.domain.student.service.StudentService;
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

@WebMvcTest(AcademicRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class AcademicRecordControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcademicRecordService academicRecordService;

    @MockBean
    private StudentAcademicRecordService studentAcademicRecordService;

    @MockBean
    private StudentService studentService;

    @Test
    @DisplayName("academic record 조회 성공 시 성공 응답을 반환한다")
    void getAcademicRecord_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(academicRecordService.getAcademicRecord(studentId, 2024, 1))
                .thenReturn(new AcademicRecordResponse(
                        null,
                        new AcademicRecordResponse.Courses(List.of(), List.of())
                ));

        mockMvc.perform(get("/api/academic/record")
                        .param("year", "2024")
                        .param("semester", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("academic summary 데이터가 없으면 U02 예외를 반환한다")
    void getAcademicSummary_notFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(studentAcademicRecordService.getAcademicSummary(studentId))
                .thenThrow(new EntityNotFoundException(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND));

        mockMvc.perform(get("/api/academic/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("U02"));
    }
}
