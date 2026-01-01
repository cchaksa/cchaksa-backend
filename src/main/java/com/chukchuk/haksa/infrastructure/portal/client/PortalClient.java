package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalClient {
    @Value("${crawler.base-url}")
    private String baseUrl;

    private final WebClient webClient = WebClient.builder().build();

    public RawPortalData scrapeAll(String username, String password) {
        String uri = "/scrape";
        long t0 = LogTime.start();

        try {
            return webClient.post()
                    .uri(baseUrl + uri)
                    .header("Content-Type", "application/json")
                    .bodyValue(new LoginRequest(username, password))
                    .retrieve()
                    .bodyToMono(RawPortalData.class)
                    .block();

        } catch (WebClientResponseException e) {
            logHttpError(uri, t0, e);
            throw new PortalScrapeException(mapHttpStatus(e.getStatusCode()), e);

        } catch (Exception e) {
            long tookMs = LogTime.elapsedMs(t0);
            log.error("[EXT] method=POST uri={} unexpected_error took_ms={}", uri, tookMs, e);
            throw new PortalScrapeException(ErrorCode.PORTAL_SCRAPE_FAILED, e);
        }
    }

    private record LoginRequest(String username, String password) {}

    private void logHttpError(String uri, long t0, WebClientResponseException e) {
        long tookMs = LogTime.elapsedMs(t0);
        int status = e.getStatusCode().value();

        if (status >= 500) log.error("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);
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
