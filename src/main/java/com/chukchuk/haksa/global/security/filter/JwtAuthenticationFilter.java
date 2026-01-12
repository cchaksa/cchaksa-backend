package com.chukchuk.haksa.global.security.filter;

import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.service.CustomUserDetailsService;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AuthTokenCache authTokenCache;

    private static final List<String> WHITELIST_PATHS = List.of(
            "/", "/v3/api-docs", "/swagger", "/webjars", "/swagger-config", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (WHITELIST_PATHS.stream().anyMatch(p ->
                p.equals("/") ? path.equals("/") : path.startsWith(p)
        )) {
            log.info("Bypassing JWT filter for swagger: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromHeader(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.parseToken(token);
            String userId = claims.getSubject();

            String tokenHash = hashToken(token);
            UserDetails userDetails = null;
            if (!"hash_err".equals(tokenHash)) {
                userDetails = authTokenCache.get(tokenHash);
            }
            if (userDetails == null) {
                userDetails = userDetailsService.loadUserByUsername(userId);
                if (!"hash_err".equals(tokenHash)) {
                    authTokenCache.put(tokenHash, userDetails);
                }
            }
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (ExpiredJwtException e) {
            request.setAttribute("exception", "expired");
            throw new InsufficientAuthenticationException("Access token expired", e);
        } catch (JwtException e) {
            request.setAttribute("exception", "invalid");
            throw new InsufficientAuthenticationException("Invalid token", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return "hash_err";
        }
    }

//    private String extractTokenFromCookie(HttpServletRequest request) {
//        if (request.getCookies() == null) return null;
//
//        for (Cookie cookie : request.getCookies()) {
//            if ("accessToken".equals(cookie.getName())) {
//                return cookie.getValue();
//            }
//        }
//        return null;
//    }
}
