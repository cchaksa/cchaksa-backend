package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    public String getCode() {
        return errorCode.code();
    }

    public HttpStatus getStatus() {
        return errorCode.status();
    }
}