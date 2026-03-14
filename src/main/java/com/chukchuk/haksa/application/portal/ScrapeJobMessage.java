package com.chukchuk.haksa.application.portal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ScrapeJobMessage(
        @JsonProperty("job_id")
        String job_id,
        @JsonProperty("user_id")
        String user_id,
        @JsonProperty("portal_type")
        String portal_type,
        @JsonProperty("request_payload")
        RequestPayload request_payload,
        @JsonProperty("requested_at")
        Instant requested_at
) {

    public record RequestPayload(
            String username,
            String password
    ) {}
}
