package com.chukchuk.haksa.infrastructure.portal.exception;

import lombok.Getter;

/** 스크래핑 결과 위치 검증 또는 S3 payload 조회 실패와 재시도 가능 여부를 나타낸다. */
@Getter
public class ScrapeResultPayloadAccessException extends RuntimeException {

  private final String errorCode;
  private final boolean retryable;

  /**
   * 저장소 오류 코드, 원인 설명과 재시도 가능 여부로 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param message 응답 메시지
   * @param retryable 같은 위치 조회를 다시 시도할 수 있는지 여부
   */
  public ScrapeResultPayloadAccessException(String errorCode, String message, boolean retryable) {
    super(message);
    this.errorCode = errorCode;
    this.retryable = retryable;
  }

  /**
   * 저장소 오류 정보와 하위 S3 또는 SDK 예외를 보존해 생성한다.
   *
   * @param errorCode 오류 코드
   * @param message 응답 메시지
   * @param retryable 같은 위치 조회를 다시 시도할 수 있는지 여부
   * @param cause 원인 예외
   */
  public ScrapeResultPayloadAccessException(
      String errorCode, String message, boolean retryable, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.retryable = retryable;
  }
}
