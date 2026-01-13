package com.chukchuk.haksa.global.security.filter;

import com.chukchuk.haksa.global.security.AuthCookieNames;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.service.CustomUserDetailsService;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private AuthTokenCache authTokenCache;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider, userDetailsService, authTokenCache);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cookieTokenHasPriorityOverHeaderToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/student/profile");
        request.setCookies(new Cookie(AuthCookieNames.ACCESS, "cookie-token"));
        request.addHeader("Authorization", "Bearer header-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        DefaultClaims claims = new DefaultClaims();
        claims.setSubject("user-123");

        when(jwtProvider.parseToken("cookie-token")).thenReturn(claims);

        UserDetails userDetails = User.withUsername("user-123")
                .password("pw")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("user-123")).thenReturn(userDetails);

        when(authTokenCache.getOrLoad(eq("user-123"), eq("cookie-token"), ArgumentMatchers.<Supplier<UserDetails>>any()))
                .thenAnswer(invocation -> {
                    Supplier<UserDetails> loader = invocation.getArgument(2);
                    return loader.get();
                });

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtProvider).parseToken("cookie-token");
        verify(jwtProvider, never()).parseToken("header-token");
        verify(authTokenCache).getOrLoad(eq("user-123"), eq("cookie-token"), any());
        verify(filterChain).doFilter(request, response);
    }
}
