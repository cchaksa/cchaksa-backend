package com.chukchuk.haksa.domain.student.model;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 업무 처리에서 사용할 성적 type 값을 정의한다. */
@Getter
@Slf4j
@RequiredArgsConstructor
public enum GradeType {
  A_PLUS("A+", 4.5),
  A0("A0", 4.0),
  B_PLUS("B+", 3.5),
  B0("B0", 3.0),
  C_PLUS("C+", 2.5),
  C0("C0", 2.0),
  D_PLUS("D+", 1.5),
  D0("D0", 1.0),
  F("F", 0.0),
  P("P", 0.0),
  NP("NP", 0.0),
  IP("IP", 0.0), // In Progress
  R("R", 0.0);

  private final String value;
  private final double gradePoint;

  /**
   * 학점을 취득한 통과 성적인지 확인한다.
   *
   * @return 실패·미통과·처리 중 성적이 아니면 {@code true}
   */
  public boolean isPassingGrade() {
    return this != F && this != NP && this != IP;
  }

  /**
   * 성적 처리가 완료된 상태인지 확인한다.
   *
   * @return 성적이 {@code IP}가 아니면 {@code true}
   */
  public boolean isCompleted() {
    return this != IP;
  }

  /**
   * 포털 성적 문자열에 대응하는 성적 유형을 반환한다.
   *
   * @param value 성적 유형
   * @return 성적 type 결과
   * @throws CommonException 알려진 성적 유형과 일치하지 않는 값인 경우
   */
  public static GradeType from(String value) {
    if (value == null || value.isBlank()) {
      return GradeType.IP; // 성적이 없는 경우 In Progress 처리
    }

    return Arrays.stream(values())
        .filter(v -> v.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> {
              // 로그 추가
              log.error("[GradeType] Unknown grade: {}", value);
              // log.warn("[GradeType] Unknown grade: '{}'", value);
              return new CommonException(ErrorCode.INVALID_GRADE_TYPE);
            });
  }
}
