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
            new OperationRef("/api/admin/test-options", "get"),
            new OperationRef("/api/admin/departments", "get"),
            new OperationRef("/api/admin/course-offerings", "get"),
            new OperationRef("/api/admin/test-lecture-evaluations/empty-semester", "post"),
            new OperationRef("/api/admin/test-lecture-evaluations/not-released", "post"),
            new OperationRef("/api/admin/test-lecture-evaluations/pending", "post"),
            new OperationRef("/api/admin/test-lecture-evaluations/skipped", "post"),
            new OperationRef("/api/admin/test-lecture-evaluations/completed", "post"),
            new OperationRef("/internal/scrape-results", "post")
    );

    private static final List<OperationRef> PROTECTED_OPERATIONS = List.of(
            new OperationRef("/portal/link", "post"),
            new OperationRef("/portal/link/jobs/{jobId}", "get"),
            new OperationRef("/portal/link/jobs/{jobId}/summary", "get"),
            new OperationRef("/portal/link/jobs/{jobId}/duration", "get"),
            new OperationRef("/api/users/analytics-id", "get"),
            new OperationRef("/api/users/me", "get"),
            new OperationRef("/api/users/delete", "delete"),
            new OperationRef("/api/student/target-gpa", "post"),
            new OperationRef("/api/student/profile", "get"),
            new OperationRef("/api/student/reset", "post"),
            new OperationRef("/api/semester", "get"),
            new OperationRef("/api/semester/grades", "get"),
            new OperationRef("/api/graduation/progress", "get"),
            new OperationRef("/api/academic/summary", "get"),
            new OperationRef("/api/academic/record", "get"),
            new OperationRef("/api/lecture-evaluations/required", "get"),
            new OperationRef("/api/lecture-evaluations", "post"),
            new OperationRef("/api/lecture-evaluations/skip", "post")
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
    void userApiResponsesUseDedicatedWrappers() throws Exception {
        JsonNode apiDocs = apiDocs();

        assertJsonResponseRef(apiDocs, "/api/users/me", "get", "200", "MeApiResponse");
        assertThat(apiDocs.path("components").path("schemas").path("MeApiResponse")
                .path("properties").path("data").path("$ref").asText())
                .isEqualTo("#/components/schemas/MeResponse");
        assertThat(apiDocs.path("components").path("schemas").path("MeApiResponse")
                .path("properties").path("message").path("type").asText())
                .isEqualTo("string");
        assertThat(apiDocs.path("components").path("schemas").path("MeResponse")
                .path("properties").path("isPortalLinked").path("type").asText())
                .isEqualTo("boolean");
    }

    @Test
    void generatedOpenApiMeResponseMatchesSchemaShape() throws Exception {
        JsonNode apiDocs = apiDocs();
        JsonNode schemas = apiDocs.path("components").path("schemas");

        assertThat(schemas.path("MeApiResponse").path("properties").path("data").path("$ref").asText())
                .isEqualTo("#/components/schemas/MeResponse");
        assertThat(schemas.has("MeResponse")).isTrue();
        assertThat(schemas.path("MeResponse").path("properties").path("isPortalLinked").path("type").asText())
                .isEqualTo("boolean");
    }

    @Test
    void generatedOpenApiDocumentsMissionLiberalAreaCodeForGraduationAndAcademicRecord() throws Exception {
        JsonNode apiDocs = apiDocs();

        assertThat(apiDocs.path("paths").has("/api/graduation/progress")).isTrue();
        assertThat(apiDocs.path("paths").has("/api/academic/record")).isTrue();

        JsonNode graduationResponse = responseSchema(apiDocs, "/api/graduation/progress", "get", "200");
        JsonNode graduationData = propertySchema(apiDocs, graduationResponse, "data");
        JsonNode graduationProgress = arrayItemSchema(apiDocs,
                propertySchema(apiDocs, graduationData, "graduationProgress"));
        JsonNode graduationCourse = arrayItemSchema(apiDocs, propertySchema(apiDocs, graduationProgress, "courses"));
        assertThat(propertySchema(apiDocs, graduationCourse, "liberalAreaCode").path("type").asText())
                .isEqualTo("integer");

        JsonNode academicRecordResponse = responseSchema(apiDocs, "/api/academic/record", "get", "200");
        JsonNode academicRecordData = propertySchema(apiDocs, academicRecordResponse, "data");
        JsonNode academicRecordCourses = propertySchema(apiDocs, academicRecordData, "courses");
        JsonNode studentCourse = arrayItemSchema(apiDocs, propertySchema(apiDocs, academicRecordCourses, "liberal"));
        assertThat(propertySchema(apiDocs, studentCourse, "liberalAreaCode").path("type").asText())
                .isEqualTo("integer");
    }

    @Test
    void generatedOpenApiDocumentsNotReleasedLectureEvaluationStatus() throws Exception {
        JsonNode apiDocs = apiDocs();

        JsonNode response = responseSchema(apiDocs, "/api/lecture-evaluations/required", "get", "200");
        JsonNode data = propertySchema(apiDocs, response, "data");

        assertThat(propertySchema(apiDocs, data, "evaluationStatus").path("enum").toString())
                .contains("NOT_RELEASED");
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

    private JsonNode responseSchema(JsonNode apiDocs, String path, String method, String responseCode) {
        JsonNode response = apiDocs.path("paths").path(path).path(method).path("responses").path(responseCode);
        JsonNode schema = response.path("content").path("application/json").path("schema");
        return resolveSchema(apiDocs, schema);
    }

    private JsonNode propertySchema(JsonNode apiDocs, JsonNode schema, String propertyName) {
        JsonNode property = resolveSchema(apiDocs, schema).path("properties").path(propertyName);
        assertThat(property.isMissingNode()).as("schema property %s should exist", propertyName).isFalse();
        return resolveSchema(apiDocs, property);
    }

    private JsonNode arrayItemSchema(JsonNode apiDocs, JsonNode schema) {
        JsonNode arraySchema = resolveSchema(apiDocs, schema);
        assertThat(arraySchema.path("type").asText()).isEqualTo("array");
        return resolveSchema(apiDocs, arraySchema.path("items"));
    }

    private JsonNode resolveSchema(JsonNode apiDocs, JsonNode schema) {
        if (schema.has("$ref")) {
            return componentSchema(apiDocs, schema.path("$ref").asText());
        }
        return schema;
    }

    private JsonNode componentSchema(JsonNode apiDocs, String ref) {
        assertThat(ref).startsWith("#/components/schemas/");
        String schemaName = ref.substring("#/components/schemas/".length());
        JsonNode schema = apiDocs.path("components").path("schemas").path(schemaName);
        assertThat(schema.isMissingNode()).as("schema %s should exist", schemaName).isFalse();
        return schema;
    }

    private record OperationRef(String path, String method) {
    }
}
