package com.chukchuk.haksa.domain.user.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import com.chukchuk.haksa.global.security.service.KakaoOidcService;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final KakaoOidcService kakaoOidcService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    @Transactional
    public AuthDto.SignInTokenResponse signInWithKakao(UserDto.SignInRequest signInRequest) {
        Claims claims = verifyKakaoToken(signInRequest);
        String email = extractEmail(claims);

        User user = findOrCreateUser(email);
        return generateSignInResponse(user);
    }

    @Transactional
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }

    /* private method */
    private Claims verifyKakaoToken(UserDto.SignInRequest request) {
        return kakaoOidcService.verifyIdToken(request.id_token(), request.nonce());
    }

    private String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    private User findOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .profileNickname("Unknown User")
                                .build()
                ));
    }

    private AuthDto.SignInTokenResponse generateSignInResponse(User user) {
        String userId = user.getId().toString();
        String accessToken = jwtProvider.createAccessToken(userId, user.getEmail(), "USER");
        AuthDto.RefreshTokenWithExpiry refresh = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, refresh.token(), refresh.expiry());

        return new AuthDto.SignInTokenResponse(accessToken, refresh.token(), user.getPortalConnected());
    }
}
