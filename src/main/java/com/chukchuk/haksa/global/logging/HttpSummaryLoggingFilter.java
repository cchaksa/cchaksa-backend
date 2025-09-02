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
import java.util.concurrent.ThreadLocalRandom;

/** 요청/응답 요약 + 샘플링 + 레벨 매핑 */
@Component
@EnableConfigurationProperties(LoggingProperties.class)
@Order(/** SecurityFilterChain 이후 = */ 20)
public class HttpSummaryLoggingFilter extends OncePerRequestFilter {

    private static final Logger HTTP = LoggerFactory.getLogger("HTTP");
    private final LoggingProperties props;

    public HttpSummaryLoggingFilter(LoggingProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        chain.doFilter(req, res);
        long took = System.currentTimeMillis() - start;

        int status = res.getStatus();
        HttpStatusLevelMapper.Level level = HttpStatusLevelMapper.map(status, req);

        // 샘플링: 고트래픽 URI만 적용 (예: /api/v1/list, /api/v1/search)
        boolean heavy = isHeavy(req.getRequestURI());
        double rate = props.getSampling().getHeavyEndpoints();
        if (heavy && ThreadLocalRandom.current().nextDouble() >= rate) return;

        String userId = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anon";
        String userIdHash = HashUtil.sha256Short(userId);
        String ip = NetUtil.clientIp(req);
        String ipShort = NetUtil.shorten(ip);
        String cid = MDC.get(CorrelationIdFilter.MDC_KEY);

        String msg = "http_summary method={} uri={} status={} took_ms={} userIdHash={} ip={} corrId={}";
        switch (level) {
            case ERROR -> HTTP.error(msg, req.getMethod(), req.getRequestURI(), status, took, userIdHash, ipShort, cid);
            case WARN  -> HTTP.warn (msg, req.getMethod(), req.getRequestURI(), status, took, userIdHash, ipShort, cid);
            default    -> HTTP.info (msg, req.getMethod(), req.getRequestURI(), status, took, userIdHash, ipShort, cid);
        }
    }

    private boolean isHeavy(String uri) {
        if (uri == null) return false;
        return uri.startsWith("/api/v1/list") || uri.startsWith("/api/v1/search");
    }
}

