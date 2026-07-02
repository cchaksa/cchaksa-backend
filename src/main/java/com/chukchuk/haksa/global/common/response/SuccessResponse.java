package com.chukchuk.haksa.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Schema(description = "성공 응답 포맷")
public class SuccessResponse<T> {

    @Schema(description = "성공 여부", example = "true", required = true)
    private final boolean success = true;

    @Schema(description = "응답 데이터", required = true)
    private final T data;

    @Schema(description = "메시지", example = "요청 성공")
    private final String message;

    public SuccessResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }

    public static <T> SuccessResponse<T> of(T data, String message) {
        return new SuccessResponse<>(data, message);
    }

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(data, "요청 성공");
    }
}
