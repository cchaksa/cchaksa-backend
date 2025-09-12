package com.chukchuk.haksa.infrastructure.portal.exception;

import com.chukchuk.haksa.global.exception.type.BaseException;
import com.chukchuk.haksa.global.exception.code.ErrorCode;

public class PortalLoginException extends BaseException {

    public PortalLoginException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PortalLoginException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}