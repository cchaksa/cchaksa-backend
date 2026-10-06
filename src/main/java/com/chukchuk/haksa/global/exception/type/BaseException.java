package com.chukchuk.haksa.global.exception.type;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** API 오류 코드와 HTTP 상태를 함께 전달하는 애플리케이션 예외의 기반 타입이다. */
@Getter
public abstract class BaseException extends RuntimeException {
  private final ErrorCode errorCode;

  /**
   * 오류 코드의 기본 메시지를 사용하는 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   */
  protected BaseException(ErrorCode errorCode) {
    super(errorCode.message());
    this.errorCode = errorCode;
  }

  /**
   * 오류 코드의 기본 메시지와 내부 원인을 보존하는 예외를 생성한다.
   *
   * @param errorCode 오류 코드
   * @param cause 원인 예외
   */
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
