package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;

/** 별도 예외 타입이 필요하지 않은 요청 또는 업무 규칙 위반을 나타낸다. */
public class CommonException extends BaseException {
  /**
   * 지정한 오류 코드와 기본 메시지로 업무 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   */
  public CommonException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 지정한 오류 코드와 내부 원인을 보존하는 업무 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param cause 원인 예외
   */
  public CommonException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
