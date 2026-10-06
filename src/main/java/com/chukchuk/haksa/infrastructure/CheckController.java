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

/** 애플리케이션 상태 확인과 Sentry 연동 점검용 HTTP endpoint를 제공한다. */
@RestController
@RequestMapping
public class CheckController {

  /**
   * 애플리케이션의 정상 동작 여부를 반환한다.
   *
   * @return 애플리케이션이 요청을 처리할 수 있음을 나타내는 {@code ok}
   */
  @Operation(
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content =
                  @Content(
                      mediaType = MediaType.TEXT_PLAIN_VALUE,
                      schema = @Schema(implementation = String.class))))
  @GetMapping("/health")
  public String health() {
    return "ok";
  }

  /**
   * Sentry 오류 수집 연동을 확인하기 위한 예외를 발생시킨다.
   *
   * @throws RuntimeException Sentry 수집 확인을 위해 항상 발생한다
   */
  @Operation(
      responses =
          @ApiResponse(
              responseCode = "500",
              description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR)",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ErrorResponseWrapper.class))))
  @GetMapping("/sentry-test")
  public void sentryTest() {
    throw new RuntimeException("SENTRY_TEST_DEV");
  }
}
