package com.chukchuk.haksa.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Schema(description = "API 실패 응답 포맷")
public class ErrorResponse {

    @Schema(description = "성공 여부", example = "false", required = true)
    private final boolean success = false;

    @Schema(description = "에러 상세")
    private final ErrorDetail error;

    protected ErrorResponse(ErrorDetail error) {
        this.error = error;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message, null));
    }

    public static ErrorResponse of(String code, String message, Object details) {
        return new ErrorResponse(new ErrorDetail(code, message, details));
    }
}
