package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLinkJobQueryService;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalJobQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalJobQueryControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalLinkJobQueryService portalLinkJobQueryService;

    @Test
    @DisplayName("본인 job은 조회할 수 있다")
    void getJobStatus_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(portalLinkJobQueryService.getJobStatus(userId, "job-1"))
                .thenReturn(new PortalLinkDto.JobStatusResponse(
                        "job-1",
                        "suwon",
                        "queued",
                        null,
                        null,
                        null,
                        Instant.parse("2026-03-14T10:00:00Z"),
                        Instant.parse("2026-03-14T10:00:00Z"),
                        null
                ));

        mockMvc.perform(get("/portal/link/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.job_id").value("job-1"))
                .andExpect(jsonPath("$.data.status").value("queued"));
    }

    @Test
    @DisplayName("타 사용자 job 조회는 404를 반환한다")
    void getJobStatus_notFound() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(portalLinkJobQueryService.getJobStatus(eq(userId), eq("job-2")))
                .thenThrow(new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));

        mockMvc.perform(get("/portal/link/jobs/job-2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.SCRAPE_JOB_NOT_FOUND.code()));
    }
}
