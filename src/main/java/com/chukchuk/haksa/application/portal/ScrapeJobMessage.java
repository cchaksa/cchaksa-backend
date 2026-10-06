package com.chukchuk.haksa.application.portal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 워커에 전달할 스크래핑 작업 식별자와 포털 로그인 payload를 표현한다.
 *
 * @param jobId 작업 식별자
 * @param userId 사용자 식별자
 * @param portalType 포털 유형
 * @param requestPayload 워커가 포털에 로그인할 자격 증명
 * @param requestedAt 작업이 접수된 ISO-8601 시각
 */
public record ScrapeJobMessage(
    @JsonProperty("job_id") String jobId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("portal_type") String portalType,
    @JsonProperty("request_payload") RequestPayload requestPayload,
    @JsonProperty("requested_at") String requestedAt) {

  /**
   * 워커의 포털 로그인 요청에 사용할 자격 증명을 표현한다.
   *
   * @param username 포털 로그인 아이디
   * @param password 포털 비밀번호
   */
  public record RequestPayload(String username, String password) {}
}
