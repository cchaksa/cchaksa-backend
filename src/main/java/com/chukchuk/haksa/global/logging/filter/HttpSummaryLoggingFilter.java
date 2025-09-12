package com.chukchuk.haksa.global.logging.filter;

import com.chukchuk.haksa.global.logging.policy.HttpStatusLevelMapper;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.global.logging.config.LoggingProperties;
import com.chukchuk.haksa.global.logging.util.NetUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP 요청/응답 요약 로그 필터.
 * - exclude 경로/확장자/정규식은 로깅 완전 제외(skipLog=true)
 * - 샘플링(설정 기반 enabled/rate/patterns)
 * - 상태코드→레벨 매핑(HttpStatusLevelMapper)
 * - PII 마스킹(LogSanitizer), 사용자 식별자 해시
 */
@Component
@EnableConfigurationProperties(LoggingProperties.class)
@Order(30) // SecurityFilterChain 이후 실행
public class HttpSummaryLoggingFilter extends OncePerRequestFilter {

    private static final Logger HTTP = LoggerFactory.getLogger("HTTP");
    private final LoggingProperties props;

    // 기본 제외: Swagger, Health, 정적 리소스, favicon, 로봇/에러
    private static final List<String> DEFAULT_EXCLUDE_PREFIXES = List.of(
            "/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars/swagger-ui",
            "/health", "/actuator/health", "/error"
    );
    private static final Set<String> DEFAULT_EXCLUDE_EXTS = Set.of(
            ".ico", ".css", ".js", ".map", ".png", ".jpg", ".jpeg", ".svg", ".gif",
            ".woff", ".woff2", ".ttf", ".eot"
    );

    public HttpSummaryLoggingFilter(LoggingProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final String uriRaw = nvl(req.getRequestURI());
        final String uri = LogSanitizer.clean(
                (req.getQueryString() == null || req.getQueryString().isBlank())
                        ? uriRaw : (uriRaw + "?" + req.getQueryString())
        );

        // ===== 1) 전역 제외 규칙: skipLog 표시 & 이 필터 로깅도 건너뜀 =====
        boolean shouldSkip = shouldSkipLogging(uri);
        if (shouldSkip) MDC.put("skipLog", "true");
        final long start = System.currentTimeMillis();

        try {
            chain.doFilter(req, res);
        } finally {
            // 응답 후 처리(로그 여부 판단/출력 포함)
            try {
                if (!shouldSkip) {
                    writeSummaryIfSampled(req, res, start, uri);
                }
            } finally {
                if (shouldSkip) MDC.remove("skipLog");
            }
        }
    }

    /* ===== helpers ===== */

    // 로깅 제외 여부: prefix/확장자/사용자 정의 패턴(정규식 포함)
    private boolean shouldSkipLogging(String uri) {
        if (uri.isBlank()) return true;

        // 1) 확장자 제외 (정적 파일)
        int dot = uri.lastIndexOf('.');
        if (dot > -1) {
            String ext = uri.substring(dot).toLowerCase();
            if (DEFAULT_EXCLUDE_EXTS.contains(ext)) return true;
        }

        // 2) prefix 제외
        for (String p : DEFAULT_EXCLUDE_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        // 특수 케이스
        if (uri.equals("/swagger-ui.html") || uri.equals("/favicon.ico")) return true;

        // 3) 설정 기반 사용자 제외(접두/정규식 혼용)
        List<String> extra = props.getExclude() != null ? props.getExclude().getPatterns() : List.of();
        for (String pat : extra) {
            if (pat == null || pat.isBlank()) continue;
            if (uri.startsWith(pat)) return true;
            try { if (uri.matches(pat)) return true; } catch (Exception ignore) {}
        }
        return false;
    }

    private void writeSummaryIfSampled(HttpServletRequest req, HttpServletResponse res, long start, String uri) {
        // ---- 샘플링 판단 ----
        boolean enabled = props.getSampling().isEnabled();
        double rate = clamp(props.getSampling().getRate(), 0.0, 1.0);
        boolean match = matchesAny(req.getRequestURI(), props.getSampling().getPatterns());
        if (enabled && match && ThreadLocalRandom.current().nextDouble() >= rate) {
            return; // 샘플링 탈락 → 로그 미출력
        }

        // ---- 레벨 매핑 ----
        int status = res.getStatus();
        HttpStatusLevelMapper.Level level = HttpStatusLevelMapper.map(status, req);

        // ---- 필드 준비 ----
        long took = System.currentTimeMillis() - start;
        String method = req.getMethod();
        String ip = NetUtil.shorten(NetUtil.clientIp(req));
        String userId = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anon";
        String traceId = nvl(MDC.get("traceId"));
        String spanId  = nvl(MDC.get("spanId"));

        // ---- 로그 출력 ----
        final String msg = "http_summary method={} uri={} status={} took_ms={} userId={} ip={} traceId={} spanId={}";
        switch (level) {
            case ERROR -> HTTP.error(msg, method, uri, status, took, userId, ip, traceId, spanId);
            case WARN  -> HTTP.warn (msg, method, uri, status, took, userId, ip, traceId, spanId);
            default    -> HTTP.info (msg, method, uri, status, took, userId, ip, traceId, spanId);
        }
    }

    private boolean matchesAny(String uri, List<String> patterns) {
        if (uri == null || patterns == null || patterns.isEmpty()) return false;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            if (uri.startsWith(p)) return true;
            try { if (uri.matches(p)) return true; } catch (Exception ignore) {}
        }
        return false;
    }

    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private String nvl(String s) { return (s == null) ? "" : s; }
}