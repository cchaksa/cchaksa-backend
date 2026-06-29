// 포털 링크 API의 Swagger 성공 응답 래퍼 구조를 검증하는 테스트
package com.chukchuk.haksa.domain.portal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "scraping.scheduler.enabled=false",
        "scraping.publisher.enabled=false",
        "scraping.stale.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class PortalLinkOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void portalLinkSuccessResponsesDocumentSuccessResponseWrappers() throws Exception {
        JsonNode apiDocs = apiDocs();

        assertSuccessResponseSchema(
                apiDocs,
                "/portal/link",
                "post",
                "202",
                "PortalLinkAcceptedApiResponse",
                "AcceptedResponse"
        );
        assertSuccessResponseSchema(
                apiDocs,
                "/portal/link/jobs/{jobId}",
                "get",
                "200",
                "PortalLinkJobStatusApiResponse",
                "JobStatusResponse"
        );
        assertSuccessResponseSchema(
                apiDocs,
                "/portal/link/jobs/{jobId}/summary",
                "get",
                "200",
                "PortalLinkJobSummaryApiResponse",
                "JobSummaryResponse"
        );
        assertSuccessResponseSchema(
                apiDocs,
                "/portal/link/jobs/{jobId}/duration",
                "get",
                "200",
                "PortalLinkJobDurationApiResponse",
                "JobDurationResponse"
        );
    }

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private void assertSuccessResponseSchema(
            JsonNode apiDocs,
            String path,
            String method,
            String responseCode,
            String wrapperSchema,
            String dataSchema
    ) {
        JsonNode response = apiDocs.path("paths").path(path).path(method).path("responses").path(responseCode);

        assertThat(response.path("content").has("application/json")).isTrue();
        assertThat(response.path("content").has("*/*")).isFalse();
        assertThat(response.path("content").path("application/json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/" + wrapperSchema);

        JsonNode component = apiDocs.path("components").path("schemas").path(wrapperSchema);
        assertThat(component.path("properties").has("success")).isTrue();
        assertThat(component.path("properties").path("data").path("$ref").asText())
                .isEqualTo("#/components/schemas/" + dataSchema);
        assertThat(component.path("properties").has("message")).isTrue();
    }
}
