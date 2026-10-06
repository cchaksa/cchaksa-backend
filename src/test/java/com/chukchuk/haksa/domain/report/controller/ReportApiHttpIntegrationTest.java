package com.chukchuk.haksa.domain.report.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false"
    })
@ActiveProfiles("test")
class ReportApiHttpIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private ReportRepository reportRepository;

  @Test
  @DisplayName("실행 중인 HTTP 서버에서 문의 API와 OpenAPI 계약을 확인한다")
  void reportApiAndOpenApiAreAvailableOverHttp() throws Exception {
    ResponseEntity<String> unauthorized =
        restTemplate.getForEntity(url("/api/reports"), String.class);
    assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    User user =
        userRepository.saveAndFlush(
            User.builder().email("report-http@example.com").profileNickname("reporter").build());
    String token = jwtProvider.createAccessToken(user.getId().toString(), user.getEmail(), "USER");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    try {
      ResponseEntity<String> created =
          restTemplate.exchange(
              url("/api/reports"),
              HttpMethod.POST,
              new HttpEntity<>(Map.of("title", "HTTP 문의", "content", "HTTP 문의 본문"), headers),
              String.class);

      assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(created.getHeaders().getLocation()).isNotNull();
      JsonNode createdBody = objectMapper.readTree(created.getBody());
      String reportId = createdBody.path("data").path("id").asText();

      ResponseEntity<String> list =
          restTemplate.exchange(
              url("/api/reports"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
      assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(objectMapper.readTree(list.getBody()).path("data").path("items").size())
          .isEqualTo(1);

      ResponseEntity<String> detail =
          restTemplate.exchange(
              url("/api/reports/" + reportId),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              String.class);
      assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(objectMapper.readTree(detail.getBody()).path("data").path("content").asText())
          .isEqualTo("HTTP 문의 본문");

      ResponseEntity<String> apiDocs = restTemplate.getForEntity(url("/v3/api-docs"), String.class);
      assertThat(apiDocs.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(objectMapper.readTree(apiDocs.getBody()).path("paths").has("/api/reports"))
          .isTrue();
    } finally {
      reportRepository.deleteAll(
          reportRepository
              .findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId(), PageRequest.of(0, 100))
              .getContent());
      userRepository.deleteById(user.getId());
    }
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
