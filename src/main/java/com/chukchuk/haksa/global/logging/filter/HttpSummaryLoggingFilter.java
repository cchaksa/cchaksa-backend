package com.chukchuk.haksa.global.logging.filter;

import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.global.logging.util.NetUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * HTTP 요청 요약 breadcrumb 전용 필터 (Sentry-only)
 */
@Component
@Order(30)
public class HttpSummaryLoggingFilter extends OncePerRequestFilter {

    private static final Logger HTTP = LoggerFactory.getLogger("HTTP");

    // 기본 제외 경로/확장자
    private static final List<String> EXCLUDE_PREFIXES = List.of(
            "/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars/swagger-ui",
            "/health", "/actuator/health", "/error"
    );
    private static final Set<String> EXCLUDE_EXTS = Set.of(
            ".ico", ".css", ".js", ".map", ".png", ".jpg", ".jpeg", ".svg", ".gif",
            ".woff", ".woff2", ".ttf", ".eot"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            writeSummary(req, res, start);
        }
    }

    private void writeSummary(HttpServletRequest req, HttpServletResponse res, long start) {
        String uriRaw = nvl(req.getRequestURI());
        if (shouldSkip(uriRaw)) return;

        String uri = LogSanitizer.clean(
                (req.getQueryString() == null || req.getQueryString().isBlank())
                        ? uriRaw
                        : uriRaw + "?" + req.getQueryString()
        );

        long took = System.currentTimeMillis() - start;

        String msg = "http_summary method={} uri={} status={} took_ms={} userId={} ip={} traceId={} spanId={}";

        HTTP.info(
                msg,
                req.getMethod(),
                uri,
                res.getStatus(),
                took,
                nvl(MDC.get("userId")),
                NetUtil.shorten(NetUtil.clientIp(req)),
                nvl(MDC.get("traceId")),
                nvl(MDC.get("spanId"))
        );
    }

    private boolean shouldSkip(String uri) {
        if (uri == null || uri.isBlank()) return true;

        int dot = uri.lastIndexOf('.');
        if (dot > -1 && EXCLUDE_EXTS.contains(uri.substring(dot).toLowerCase())) {
            return true;
        }

        for (String p : EXCLUDE_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }

        return uri.equals("/favicon.ico") || uri.equals("/swagger-ui.html");
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}