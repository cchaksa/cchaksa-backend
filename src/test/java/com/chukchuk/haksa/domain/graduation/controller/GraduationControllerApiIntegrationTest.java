package com.chukchuk.haksa.domain.graduation.controller;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.service.GraduationService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
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

@WebMvcTest(GraduationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GraduationControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GraduationService graduationService;

    @Test
    @DisplayName("graduation progress 조회 성공 시 성공 응답을 반환한다")
    void getGraduationProgress_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(graduationService.getGraduationProgress(studentId))
                .thenReturn(new GraduationProgressResponse(List.of()));

        mockMvc.perform(get("/api/graduation/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("졸업요건 데이터가 없으면 G02 예외를 반환한다")
    void getGraduationProgress_notFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        authenticate(userId, studentId);

        when(graduationService.getGraduationProgress(studentId))
                .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));

        mockMvc.perform(get("/api/graduation/progress"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("G02"));
    }
}
