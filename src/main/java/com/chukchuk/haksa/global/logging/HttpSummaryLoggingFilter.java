package com.chukchuk.haksa.global.logging;

import com.chukchuk.haksa.global.logging.util.HashUtil;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP 요청/응답 요약 로그 필터.
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

    public HttpSummaryLoggingFilter(LoggingProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final long start = System.currentTimeMillis();
        chain.doFilter(req, res);
        final long took = System.currentTimeMillis() - start;

        // ---- 샘플링 결정 ----
        boolean enabled = props.getSampling().isEnabled();
        double rate = clamp(props.getSampling().getRate(), 0.0, 1.0);
        boolean match = matchesAny(req.getRequestURI(), props.getSampling().getPatterns());

        // 샘플링: enabled && match일 때만 적용 (rate=1.0이면 모두 로깅)
        if (enabled && match && ThreadLocalRandom.current().nextDouble() >= rate) {
            return; // 로그 생략
        }

        // ---- 레벨 매핑 ----
        int status = res.getStatus();
        HttpStatusLevelMapper.Level level = HttpStatusLevelMapper.map(status, req);

        // ---- 필드 준비 (마스킹/축약) ----
        String method = req.getMethod();
        String uri = sanitizeUri(req);
        String ip = NetUtil.shorten(NetUtil.clientIp(req));
        String userId = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anon";
        String userHash = HashUtil.sha256Short(userId);

        // 추적키: Micrometer/Brave가 넣는 traceId/spanId가 있으면 사용
        String traceId = nvl(MDC.get("traceId"));
        String spanId  = nvl(MDC.get("spanId"));

        // ---- 로깅 ----
        // 한 줄 요약: 메서드, URI, 상태, 소요(ms), 사용자해시, IP, 추적키
        final String msg = "http_summary method={} uri={} status={} took_ms={} userIdHash={} ip={} traceId={} spanId={}";
        switch (level) {
            case ERROR -> HTTP.error(msg, method, uri, status, took, userHash, ip, traceId, spanId);
            case WARN  -> HTTP.warn (msg, method, uri, status, took, userHash, ip, traceId, spanId);
            default    -> HTTP.info (msg, method, uri, status, took, userHash, ip, traceId, spanId);
        }
    }

    // --- helpers ---

    private String sanitizeUri(HttpServletRequest req) {
        String base = nvl(req.getRequestURI());
        String q = req.getQueryString();
        String full = q == null || q.isBlank() ? base : (base + "?" + q);
        return LogSanitizer.clean(full);
    }

    private boolean matchesAny(String uri, List<String> patterns) {
        if (uri == null || patterns == null || patterns.isEmpty()) return false;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            // 1) 접두사 매칭
            if (uri.startsWith(p)) return true;
            // 2) 정규식 매칭
            try {
                if (uri.matches(p)) return true;
            } catch (Exception ignored) { /* 잘못된 정규식은 패스 */ }
        }
        return false;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }
}