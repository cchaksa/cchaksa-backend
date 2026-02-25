package com.chukchuk.haksa.domain.user.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.SocialAccountRepository;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AcademicCache academicCache;

    @Mock
    private AuthTokenCache authTokenCache;

    @Mock
    private OidcService appleOidcService;

    @Captor
    private ArgumentCaptor<OidcProvider> providerCaptor;

    @Test
    void signIn_usesProviderFromRequest_andReturnsTokens() {
        Claims claims = Jwts.claims().setSubject("apple-sub");
        claims.put("email", "apple@example.com");

        when(appleOidcService.verifyIdToken("id-token", "nonce")).thenReturn(claims);
        when(socialAccountRepository.findByProviderAndSocialId(any(), any()))
                .thenReturn(Optional.empty());

        UUID userId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userId)
                .email("apple@example.com")
                .profileNickname("Unknown User")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtProvider.createAccessToken(userId.toString(), "apple@example.com", "USER"))
                .thenReturn("access");
        when(jwtProvider.createRefreshToken(userId.toString()))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("refresh", new Date()));

        UserService userService = new UserService(
                userRepository,
                socialAccountRepository,
                jwtProvider,
                refreshTokenService,
                academicCache,
                authTokenCache,
                Map.of(OidcProvider.APPLE, appleOidcService)
        );

        UserDto.SignInRequest request = new UserDto.SignInRequest(
                OidcProvider.APPLE,
                "id-token",
                "nonce"
        );

        AuthDto.SignInTokenResponse response = userService.signIn(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.isPortalLinked()).isFalse();

        verify(socialAccountRepository).findByProviderAndSocialId(providerCaptor.capture(), eq("apple-sub"));
        assertThat(providerCaptor.getValue()).isEqualTo(OidcProvider.APPLE);
        verify(refreshTokenService).save(eq(userId.toString()), eq("refresh"), any(Date.class));
    }
}
