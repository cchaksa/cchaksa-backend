package com.chukchuk.haksa.infrastructure.portal.model;

import org.springframework.lang.Nullable;

/** 개설 과목 생성 정보와 선택적으로 매칭된 기존 학사 과목 정보를 함께 전달한다. */
public class MergedOfferingAcademic {
  private final PortalOfferingCreationData offering;
  private final @Nullable PortalCourseInfo academic;

  /**
   * 개설 과목과 매칭된 학사 과목을 하나의 조회 결과로 생성한다.
   *
   * @param offering 저장할 개설 과목 정보
   * @param academic 매칭된 기존 학사 과목이며 없을 수 있음
   */
  public MergedOfferingAcademic(PortalOfferingCreationData offering, PortalCourseInfo academic) {
    this.offering = offering;
    this.academic = academic;
  }

  public PortalOfferingCreationData getOffering() {
    return offering;
  }

  public PortalCourseInfo getAcademic() {
    return academic;
  }
}
