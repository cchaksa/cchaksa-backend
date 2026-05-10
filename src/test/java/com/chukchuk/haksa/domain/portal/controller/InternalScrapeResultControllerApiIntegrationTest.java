package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.ScrapeResultCallbackService;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalScrapeResultController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalScrapeResultControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapeResultCallbackService scrapeResultCallbackService;

    @Test
    @DisplayName("internal scrape result 요청은 callback service에 raw body와 헤더를 전달한다")
    void handleCallback_success() throws Exception {
        String body = """
                {
                  "job_id":"job-1",
                  "status":"succeeded",
                  "result_s3_key":"callbacks/job-1/result.json",
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """;

        mockMvc.perform(post("/internal/scrape-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", "2026-03-14T10:01:00Z")
                        .header("X-Signature", "signature")
                        .header("X-Callback-Attempt", "2")
                        .header("X-Request-Id", "req-1")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(scrapeResultCallbackService).handleCallback(eq(body), eq("2026-03-14T10:01:00Z"), eq("signature"), eq("2"), eq("req-1"));
    }
}
