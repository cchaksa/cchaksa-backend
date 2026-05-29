// 전체 API Swagger 응답 계약이 실제 공통 응답 구조와 맞는지 검증하는 테스트
package com.chukchuk.haksa.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "scraping.scheduler.enabled=false",
        "scraping.publisher.enabled=false",
        "scraping.stale.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class OpenApiResponseContractTest {

    private static final List<OperationRef> PUBLIC_OPERATIONS = List.of(
            new OperationRef("/health", "get"),
            new OperationRef("/sentry-test", "get"),
            new OperationRef("/api/users/signin", "post"),
            new OperationRef("/api/auth/refresh", "post"),
            new OperationRef("/internal/scrape-results", "post")
    );

    private static final List<OperationRef> PROTECTED_OPERATIONS = List.of(
            new OperationRef("/portal/link", "post"),
            new OperationRef("/portal/link/jobs/{jobId}", "get"),
            new OperationRef("/portal/link/jobs/{jobId}/summary", "get"),
            new OperationRef("/api/users/analytics-id", "get"),
            new OperationRef("/api/users/delete", "delete"),
            new OperationRef("/api/student/target-gpa", "post"),
            new OperationRef("/api/student/profile", "get"),
            new OperationRef("/api/student/reset", "post"),
            new OperationRef("/api/semester", "get"),
            new OperationRef("/api/semester/grades", "get"),
            new OperationRef("/api/graduation/progress", "get"),
            new OperationRef("/api/academic/summary", "get"),
            new OperationRef("/api/academic/record", "get")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiSecurityMatchesRuntimeAuthenticationRequirements() throws Exception {
        JsonNode apiDocs = apiDocs();

        assertThat(apiDocs.path("security").isMissingNode() || apiDocs.path("security").isEmpty()).isTrue();

        for (OperationRef operation : PUBLIC_OPERATIONS) {
            assertThat(operation(apiDocs, operation).path("security").isMissingNode()
                    || operation(apiDocs, operation).path("security").isEmpty())
                    .as("%s %s should not require bearerAuth", operation.method(), operation.path())
                    .isTrue();
        }

        for (OperationRef operation : PROTECTED_OPERATIONS) {
            JsonNode security = operation(apiDocs, operation).path("security");
            assertThat(security.isArray()).as("%s %s security", operation.method(), operation.path()).isTrue();
            assertThat(security.toString()).contains("bearerAuth");
        }
    }

    @Test
    void protectedOperationsDocumentAuthenticationRequiredResponse() throws Exception {
        JsonNode apiDocs = apiDocs();

        for (OperationRef operation : PROTECTED_OPERATIONS) {
            assertJsonResponseRef(apiDocs, operation.path(), operation.method(), "401", "ErrorResponseWrapper");
        }
    }

    @Test
    void callbackHealthAndSentryResponsesMatchRuntimeShape() throws Exception {
        JsonNode apiDocs = apiDocs();

        assertJsonResponseRef(apiDocs, "/internal/scrape-results", "post", "200", "PortalLinkCallbackApiResponse");
        assertJsonResponseRef(apiDocs, "/internal/scrape-results", "post", "401", "ErrorResponseWrapper");

        assertTextResponse(apiDocs, "/health", "get", "200");

        JsonNode sentryResponses = operation(apiDocs, new OperationRef("/sentry-test", "get")).path("responses");
        assertThat(sentryResponses.has("200")).isFalse();
        assertJsonResponseRef(apiDocs, "/sentry-test", "get", "500", "ErrorResponseWrapper");
    }

    @Test
    void jsonResponsesDoNotUseWildcardMediaType() throws Exception {
        JsonNode apiDocs = apiDocs();

        apiDocs.path("paths").fields().forEachRemaining(pathEntry ->
                pathEntry.getValue().fields().forEachRemaining(methodEntry ->
                        methodEntry.getValue().path("responses").fields().forEachRemaining(responseEntry ->
                                assertThat(responseEntry.getValue().path("content").has("*/*"))
                                        .as("%s %s %s should not use wildcard media type",
                                                methodEntry.getKey(), pathEntry.getKey(), responseEntry.getKey())
                                        .isFalse()
                        )
                )
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

    private JsonNode operation(JsonNode apiDocs, OperationRef operation) {
        return apiDocs.path("paths").path(operation.path()).path(operation.method());
    }

    private void assertJsonResponseRef(
            JsonNode apiDocs,
            String path,
            String method,
            String responseCode,
            String schemaName
    ) {
        JsonNode response = apiDocs.path("paths").path(path).path(method).path("responses").path(responseCode);
        assertThat(response.path("content").has("application/json"))
                .as("%s %s %s should have application/json", method, path, responseCode)
                .isTrue();
        assertThat(response.path("content").path("application/json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/" + schemaName);
    }

    private void assertTextResponse(JsonNode apiDocs, String path, String method, String responseCode) {
        JsonNode response = apiDocs.path("paths").path(path).path(method).path("responses").path(responseCode);
        assertThat(response.path("content").has("text/plain")).isTrue();
        assertThat(response.path("content").path("text/plain").path("schema").path("type").asText())
                .isEqualTo("string");
    }

    private record OperationRef(String path, String method) {
    }
}
