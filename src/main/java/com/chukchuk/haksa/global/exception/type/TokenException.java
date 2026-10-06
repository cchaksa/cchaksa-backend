package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;

/** 인증 토큰의 형식, 서명, claim 또는 만료 상태가 유효하지 않음을 나타낸다. */
public class TokenException extends BaseException {
  /**
   * 토큰 오류 코드와 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   */
  public TokenException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 토큰 오류 코드와 검증 실패 원인을 보존하는 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param cause 원인 예외
   */
  public TokenException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
