package com.chukchuk.haksa.global.logging;

import com.chukchuk.haksa.global.logging.util.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(20) // SecurityFilterChain 이후, HttpSummaryLoggingFilter(30)보다 먼저 실행
public class MdcUserEnricherFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            // userId
            String userId = resolveUserId();
            if (userId == null || userId.isBlank()) userId = "anon";
            MDC.put("userId", userId);
            MDC.put("userIdHash", HashUtil.sha256Short(userId));

            // studentCode (있으면만)
            String studentCode = resolveStudentCode();
            if (studentCode != null && !studentCode.isBlank()) {
                MDC.put("studentCodeHash", HashUtil.sha256Short(studentCode));
            }

            chain.doFilter(req, res);
        } finally {
            MDC.remove("userId");
            MDC.remove("userIdHash");
            MDC.remove("studentCodeHash");
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails u) return u.getUsername();
        if (principal instanceof String s) return s;
        return auth.getName();
    }

    private String resolveStudentCode() {
        // 프로젝트 환경에 맞게 구현 (예: JWT claim, CustomUser 필드, DB 조회 등)
        return null;
    }
}