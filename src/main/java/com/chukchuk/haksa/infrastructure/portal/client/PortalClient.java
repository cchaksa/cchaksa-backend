package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.LogTime;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalLoginException;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    public void login(String username, String password) {
        try {
            webClient.post()
                    .uri(baseUrl + "/auth")
                    .header("Content-Type", "application/json")
                    .bodyValue(new LoginRequest(username, password))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());

            String message = switch (status) {
                case UNAUTHORIZED -> "아이디나 비밀번호가 일치하지 않습니다.";
                case LOCKED -> "계정이 잠겼습니다. 포털에서 비밀번호 재발급이 필요합니다.";
                default -> "로그인 중 오류가 발생했습니다.";
            };
            throw new PortalLoginException(ErrorCode.PORTAL_LOGIN_FAILED, e);
        }
    }

    public RawPortalData scrapeAll(String username, String password) {
        String uri = "/scrape";
        long t0 = LogTime.start();
        try {
            return webClient.post().uri(baseUrl + uri)
                    .header("Content-Type", "application/json")
                    .bodyValue(new LoginRequest(username, password))
                    .retrieve().bodyToMono(RawPortalData.class).block();
        } catch (WebClientResponseException e) {
            long tookMs = LogTime.elapsedMs(t0);
            int status = e.getStatusCode().value();

            if (status >= 500)      log.error("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);
            else if (status == 429) log.warn ("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);
            else                    log.warn ("[EXT] method=POST uri={} status={} took_ms={}", uri, status, tookMs);

            ErrorCode code = switch (HttpStatus.resolve(status)) {
                case UNAUTHORIZED -> ErrorCode.PORTAL_LOGIN_FAILED;
                case LOCKED      -> ErrorCode.PORTAL_ACCOUNT_LOCKED;
                default          -> ErrorCode.PORTAL_SCRAPE_FAILED;
            };
            throw new PortalScrapeException(code, e);
        }
    }

    private record LoginRequest(String username, String password) {}
}
