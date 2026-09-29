package com.chukchuk.haksa.global.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API 서버 정보, JWT 보안 스키마 및 공통 응답 계약을 OpenAPI 문서에 구성한다. */
@Configuration
@SecuritySchemes({
  @SecurityScheme(
      name = "bearerAuth",
      type = SecuritySchemeType.HTTP,
      scheme = "bearer",
      bearerFormat = "JWT",
      description = "JWT 인증 토큰을 입력하세요"),
  @SecurityScheme(
      name = "adminSession",
      type = SecuritySchemeType.APIKEY,
      in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE,
      paramName = "cchaksa_admin_session",
      description = "관리자 로그인에서 발급한 HttpOnly 세션 쿠키")
})
public class OpenApiConfig {

  @Value("${swagger.server-url}")
  private String serverUrl;

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  /**
   * 척척학사 OpenAPI 기본 문서를 구성한다.
   *
   * @return 현재 profile 서버 URL과 API 기본 정보가 포함된 OpenAPI 문서
   */
  @Bean(name = "customOpenAPI")
  public OpenAPI customOpenApi() {
    Server server =
        new Server()
            .url(serverUrl)
            .description(
                Character.toUpperCase(activeProfile.charAt(0))
                    + activeProfile.substring(1)
                    + " Server");

    return new OpenAPI()
        .info(new Info().title("척척학사 API").version("v1").description("API 명세서"))
        .servers(List.of(server));
  }

  /**
   * 공통 API 응답 계약을 OpenAPI 문서에 반영한다.
   *
   * @return wildcard media type을 정규화하고 인증 API에 401 응답을 추가하는 customizer
   */
  @Bean
  public OpenApiCustomizer responseContractCustomizer() {
    return openApi ->
        openApi
            .getPaths()
            .forEach(
                (path, pathItem) ->
                    pathItem
                        .readOperations()
                        .forEach(
                            operation -> {
                              normalizeWildcardMediaTypes(operation);
                              documentAuthenticationFailure(operation);
                            }));
  }

  private void normalizeWildcardMediaTypes(Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      return;
    }

    responses
        .values()
        .forEach(
            response -> {
              Content content = response.getContent();
              if (content == null || !content.containsKey("*/*")) {
                return;
              }

              MediaType wildcard = content.remove("*/*");
              String mediaType =
                  isPlainText(wildcard)
                      ? org.springframework.http.MediaType.TEXT_PLAIN_VALUE
                      : org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
              content.addMediaType(mediaType, wildcard);
            });
  }

  private boolean isPlainText(MediaType mediaType) {
    Schema<?> schema = mediaType.getSchema();
    return schema != null && schema.get$ref() == null && "string".equals(schema.getType());
  }

  private void documentAuthenticationFailure(Operation operation) {
    if (!requiresAuthentication(operation)) {
      return;
    }

    ApiResponses responses = operation.getResponses();
    if (responses == null || responses.containsKey("401")) {
      return;
    }

    responses.addApiResponse(
        "401",
        new ApiResponse()
            .description("인증 실패 (AUTHENTICATION_REQUIRED)")
            .content(jsonContent("ErrorResponseWrapper")));
  }

  private boolean requiresAuthentication(Operation operation) {
    return operation.getSecurity() != null
        && operation.getSecurity().stream()
            .anyMatch(
                requirement ->
                    requirement.containsKey("bearerAuth")
                        || requirement.containsKey("adminSession"));
  }

  private Content jsonContent(String schemaName) {
    Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + schemaName);
    return new Content()
        .addMediaType(
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
            new MediaType().schema(schema));
  }
}
