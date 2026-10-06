// 포털 지정과목 배열의 수신 여부와 목록을 함께 전달한다.

package com.chukchuk.haksa.infrastructure.portal.model;

import java.util.List;

/** 지정과목 필드 수신 여부와 원본 순서를 보존한 목록을 표현한다. */
public record DesignatedCourseSnapshot(boolean received, List<DesignatedCourseData> courses) {

  /**
   * 수신 여부와 지정과목 목록을 불변 값으로 보관한다.
   *
   * @param received 지정과목 필드가 payload에 포함되었는지 여부
   * @param courses 원본 순서의 지정과목 목록
   */
  public DesignatedCourseSnapshot {
    courses = List.copyOf(courses);
  }

  /** 지정과목 필드가 수신되지 않은 구버전 payload를 표현한다. */
  public static DesignatedCourseSnapshot notReceived() {
    return new DesignatedCourseSnapshot(false, List.of());
  }

  /** 지정과목 필드를 수신한 payload를 표현한다. */
  public static DesignatedCourseSnapshot received(List<DesignatedCourseData> courses) {
    return new DesignatedCourseSnapshot(true, courses);
  }
}
