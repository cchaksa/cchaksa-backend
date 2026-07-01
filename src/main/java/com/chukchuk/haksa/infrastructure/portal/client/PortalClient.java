package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalClient {
    @Value("${crawler.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public RawPortalData scrapeAll(String username, String password) {
        String uri = "/scrape";
        long t0 = LogTime.start();

        try {
            RequestEntity<LoginRequest> request = RequestEntity
                    .post(URI.create(baseUrl + uri))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(username, password));

            RawPortalData body = restTemplate.exchange(request, RawPortalData.class).getBody();
            if (body == null) {
                throw new PortalScrapeException(ErrorCode.PORTAL_SCRAPE_FAILED);
            }
            return body;
        } catch (RestClientResponseException e) {
            logHttpError(uri, t0, e);
            throw new PortalScrapeException(mapHttpStatus(e.getStatusCode()), e);

        } catch (Exception e) {
            long tookMs = LogTime.elapsedMs(t0);
            log.warn("[EXT] method=POST uri={} unexpected_error took_ms={}", uri, tookMs, e);
            throw new PortalScrapeException(ErrorCode.PORTAL_SCRAPE_FAILED, e);
        }
    }

    private record LoginRequest(String username, String password) {}

    private void logHttpError(String uri, long t0, RestClientResponseException e) {
        long tookMs = LogTime.elapsedMs(t0);
        int status = e.getStatusCode().value();

        if (status >= 500) log.warn("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);
        else               log.warn ("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);
    }

    private ErrorCode mapHttpStatus(HttpStatusCode status) {
        if (status == null) {
            return ErrorCode.PORTAL_SCRAPE_FAILED;
        }

        return switch (status.value()) {
            case 401 -> ErrorCode.PORTAL_LOGIN_FAILED;
            case 423 -> ErrorCode.PORTAL_ACCOUNT_LOCKED;
            default  -> ErrorCode.PORTAL_SCRAPE_FAILED;
        };
    }
}
