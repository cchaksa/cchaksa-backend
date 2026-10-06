// 포털 링크 API의 Swagger 성공 응답 래퍼 구조를 검증하는 테스트

package com.chukchuk.haksa.domain.portal.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false"
    })
class PortalLinkOpenApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void portalLinkSuccessResponsesDocumentSuccessResponseWrappers() throws Exception {
    JsonNode apiDocs = apiDocs();

    assertThat(apiDocs.path("paths").has("/portal/login")).isFalse();
    assertThat(
            apiDocs
                .path("components")
                .path("schemas")
                .path("LinkRequest")
                .path("properties")
                .has("portal_verification_token"))
        .isFalse();
    assertSuccessResponseSchema(
        apiDocs,
        "/portal/link",
        "post",
        "202",
        "PortalLinkAcceptedApiResponse",
        "AcceptedResponse");
    assertSuccessResponseSchema(
        apiDocs,
        "/portal/link/jobs/{jobId}",
        "get",
        "200",
        "PortalLinkJobStatusApiResponse",
        "JobStatusResponse");
    assertSuccessResponseSchema(
        apiDocs,
        "/portal/link/jobs/{jobId}/summary",
        "get",
        "200",
        "PortalLinkJobSummaryApiResponse",
        "JobSummaryResponse");
    assertSuccessResponseSchema(
        apiDocs,
        "/portal/link/jobs/{jobId}/duration",
        "get",
        "200",
        "PortalLinkJobDurationApiResponse",
        "JobDurationResponse");
  }

  private JsonNode apiDocs() throws Exception {
    ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    return objectMapper.readTree(response.getBody());
  }

  private void assertSuccessResponseSchema(
      JsonNode apiDocs,
      String path,
      String method,
      String responseCode,
      String wrapperSchema,
      String dataSchema) {
    JsonNode response =
        apiDocs.path("paths").path(path).path(method).path("responses").path(responseCode);

    assertThat(response.path("content").has("application/json")).isTrue();
    assertThat(response.path("content").has("*/*")).isFalse();
    assertThat(
            response.path("content").path("application/json").path("schema").path("$ref").asText())
        .isEqualTo("#/components/schemas/" + wrapperSchema);

    JsonNode component = apiDocs.path("components").path("schemas").path(wrapperSchema);
    assertThat(component.path("properties").has("success")).isTrue();
    assertThat(component.path("properties").path("data").path("$ref").asText())
        .isEqualTo("#/components/schemas/" + dataSchema);
    assertThat(component.path("properties").has("message")).isTrue();
  }
}
