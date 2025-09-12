package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;

public class EntityNotFoundException extends BaseException {
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}