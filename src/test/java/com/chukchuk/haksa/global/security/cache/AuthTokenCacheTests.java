package com.chukchuk.haksa.global.security.cache;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenCacheTests {

    @Test
    void getOrLoadCachesUserDetailsByToken() {
        AuthTokenCache cache = new AuthTokenCache(60_000L);
        AtomicInteger calls = new AtomicInteger();
        UserDetails user = User.withUsername("user-1")
                .password("")
                .authorities("ROLE_USER")
                .build();

        UserDetails first = cache.getOrLoad("user-1", "token-1", () -> {
            calls.incrementAndGet();
            return user;
        });
        UserDetails second = cache.getOrLoad("user-1", "token-1", () -> {
            calls.incrementAndGet();
            return User.withUsername("other").password("").authorities("ROLE_USER").build();
        });

        assertThat(first).isSameAs(second);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void evictByUserIdRemovesAllTokensForUser() {
        AuthTokenCache cache = new AuthTokenCache(60_000L);
        AtomicInteger calls = new AtomicInteger();
        UserDetails user = User.withUsername("user-1")
                .password("")
                .authorities("ROLE_USER")
                .build();

        cache.getOrLoad("user-1", "token-1", () -> {
            calls.incrementAndGet();
            return user;
        });
        cache.getOrLoad("user-1", "token-2", () -> {
            calls.incrementAndGet();
            return user;
        });

        cache.evictByUserId("user-1");

        UserDetails reloaded = cache.getOrLoad("user-1", "token-1", () -> {
            calls.incrementAndGet();
            return user;
        });
        assertThat(reloaded).isSameAs(user);
        assertThat(calls.get()).isEqualTo(3);
    }
}
