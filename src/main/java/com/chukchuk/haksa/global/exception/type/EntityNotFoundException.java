package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;

/** 요청한 도메인 객체를 조회할 수 없음을 나타낸다. */
public class EntityNotFoundException extends BaseException {
  /**
   * 조회 대상에 대응하는 오류 코드와 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   */
  public EntityNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 조회 실패 오류 코드와 내부 원인을 보존하는 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param cause 원인 예외
   */
  public EntityNotFoundException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
