package com.chukchuk.haksa.application.academic.dto;

/** 포털 학사 기록 동기화의 성공 여부와 실패 사유를 전달한다. */
public class SyncAcademicRecordResult {
  private final boolean isSuccess;
  private final String error;

  /**
   * 동기화 성공 여부와 실패 시 노출할 사유를 생성한다.
   *
   * @param isSuccess 학사 기록 동기화가 완료됐는지 여부
   * @param error 실패한 경우의 오류 메시지이며 성공하면 {@code null}
   */
  public SyncAcademicRecordResult(boolean isSuccess, String error) {
    this.isSuccess = isSuccess;
    this.error = error;
  }

  /**
   * 성공 결과를 생성한다.
   *
   * @return 오류가 없는 성공 결과
   */
  public static SyncAcademicRecordResult success() {
    return new SyncAcademicRecordResult(true, null);
  }

  /**
   * 실패 사유를 포함한 결과를 생성한다.
   *
   * @param error 호출자에게 전달할 동기화 실패 사유
   * @return 실패 사유가 포함된 결과
   */
  public static SyncAcademicRecordResult failure(String error) {
    return new SyncAcademicRecordResult(false, error);
  }

  /**
   * 학사 기록 동기화가 성공했는지 확인한다.
   *
   * @return 동기화 결과가 성공이면 {@code true}
   */
  public boolean isSuccess() {
    return isSuccess;
  }

  public String getError() {
    return error;
  }
}
