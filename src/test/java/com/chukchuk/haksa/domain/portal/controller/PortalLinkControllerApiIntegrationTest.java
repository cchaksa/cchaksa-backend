package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLinkJobService;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalLinkControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalLinkJobService portalLinkJobService;

    @Test
    @DisplayName("portal link 요청 성공 시 accepted 응답을 반환한다")
    void createPortalLinkJob_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(portalLinkJobService.acceptJob(eq(userId), eq("idem-1"), any(PortalLinkDto.LinkRequest.class)))
                .thenReturn(new PortalLinkDto.AcceptedResponse("job-1", "accepted", "/portal/link/jobs/job-1"));

        mockMvc.perform(post("/portal/link")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portal_type":"suwon",
                                  "username":"17019013",
                                  "password":"pw"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.job_id").value("job-1"))
                .andExpect(jsonPath("$.data.status").value("accepted"));
    }
}
