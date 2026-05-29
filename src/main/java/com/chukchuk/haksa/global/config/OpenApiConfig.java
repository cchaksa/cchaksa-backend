package com.chukchuk.haksa.global.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT 인증 토큰을 입력하세요"
)
public class OpenApiConfig {

    @Value("${swagger.server-url}")
    private String serverUrl;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server()
                .url(serverUrl)
                .description(Character.toUpperCase(activeProfile.charAt(0)) + activeProfile.substring(1) + " Server");

        return new OpenAPI()
                .info(new Info().title("척척학사 API").version("v1").description("API 명세서"))
                .servers(List.of(server));
    }

    @Bean
    public OpenApiCustomizer responseContractCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperations().forEach(operation -> {
                    normalizeWildcardMediaTypes(operation);
                    documentAuthenticationFailure(operation);
                })
        );
    }

    private void normalizeWildcardMediaTypes(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }

        responses.values().forEach(response -> {
            Content content = response.getContent();
            if (content == null || !content.containsKey("*/*")) {
                return;
            }

            MediaType wildcard = content.remove("*/*");
            String mediaType = isPlainText(wildcard)
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
        if (!requiresBearerAuth(operation)) {
            return;
        }

        ApiResponses responses = operation.getResponses();
        if (responses == null || responses.containsKey("401")) {
            return;
        }

        responses.addApiResponse("401", new ApiResponse()
                .description("인증 실패 (AUTHENTICATION_REQUIRED)")
                .content(jsonContent("ErrorResponseWrapper")));
    }

    private boolean requiresBearerAuth(Operation operation) {
        return operation.getSecurity() != null && operation.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey("bearerAuth"));
    }

    private Content jsonContent(String schemaName) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + schemaName);
        return new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(schema)
        );
    }
}
