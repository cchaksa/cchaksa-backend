package com.chukchuk.haksa.infrastructure.portal.exception;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.BaseException;

/** 포털 연결 또는 학사 데이터 동기화가 완료되지 못했음을 나타낸다. */
public class PortalScrapeException extends BaseException {

  /**
   * 포털 처리 단계에 대응하는 오류 코드와 기본 메시지로 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   */
  public PortalScrapeException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 포털 처리 오류 코드와 내부 실패 원인을 보존하는 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param cause 원인 예외
   */
  public PortalScrapeException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
