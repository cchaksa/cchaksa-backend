package com.chukchuk.haksa.global.exception;

public class ReconnectionRequiredException extends BaseException {
    public ReconnectionRequiredException() {
        super(ErrorCode.RECONNECTION_REQUIRED);
    }
}