package com.chukchuk.haksa.infrastructure;

import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CheckController {

    @Operation(
            responses = @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class)))
    )
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @Operation(
            responses = @ApiResponse(responseCode = "500", description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseWrapper.class)))
    )
    @GetMapping("/sentry-test")
    public void sentryTest() {
        throw new RuntimeException("SENTRY_TEST_DEV");
    }
}
