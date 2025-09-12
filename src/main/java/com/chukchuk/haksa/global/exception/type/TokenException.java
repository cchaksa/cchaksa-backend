package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;

public class TokenException extends BaseException {
    public TokenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TokenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}