package com.chukchuk.haksa.domain.user.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.model.SocialAccount;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.SocialAccountRepository;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final AcademicCache academicCache;
    private final AuthTokenCache authTokenCache;

    private final Map<OidcProvider, OidcService> oidcServices;

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    @Transactional
    public AuthDto.SignInTokenResponse signIn(UserDto.SignInRequest signInRequest) {
        OidcProvider provider = OidcProvider.KAKAO; // TODO: 실제 요청에서 provider 추춮
        Claims claims = verifyToken(provider, signInRequest);

        String sub = claims.getSubject();
        String email = extractEmail(claims);

        User user = findOrCreateUser(provider, sub, email, "Unknown User");
        return generateSignInResponse(user);
    }

    @Transactional
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        UUID studentId = user.getStudent().getId();
        academicCache.deleteAllByStudentId(studentId);
        authTokenCache.evictByUserId(userId.toString());
        socialAccountRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("[BIZ] user.delete.done userId={}", userId);
    }

    /**
     * 소셜 로그인 후 포털 연동 시, studentCode 기반으로 기존 User가 있는지 탐색하여 병합 시도.
     * - 기존 User가 없다면: currentUser를 그대로 사용
     * - 기존 User가 있다면:
     *   - 기존 User의 SocialAccount들을 currentUser에 연결
     *   - 기존 User의 필드값들을 currentUser에 할당
     *   - student에 연결된 기존 User를 currentUser로 변경
     *   - 기존 User 삭제 후 currentUser 리턴
     */
    @Transactional
    public User tryMergeWithExistingUser(UUID currentUserId, String studentCode) {
        User currentUser = getUserById(currentUserId);
        Optional<User> existingUserOpt = userRepository.findByStudent_StudentCode(studentCode);

        if (existingUserOpt.isEmpty()) {
            return currentUser;
        }

        User existingUser = existingUserOpt.get();

        // 소셜 계정 모두 이전
        List<SocialAccount> accounts = socialAccountRepository.findAllByUserId(existingUser.getId());
        for (SocialAccount sa : accounts) {
            sa.updateUser(currentUser);
        }

        currentUser.absorbFrom(existingUser);

        Student student = existingUser.getStudent();
        student.updateUser(currentUser);
        existingUser.setStudent(null);

        userRepository.delete(existingUser);
        log.info("[BIZ] user.merged existingUserId={} into currentUserId={}", existingUser.getId(), currentUserId);

        return currentUser;
    }

    /* private method */
    private Claims verifyToken(OidcProvider provider, UserDto.SignInRequest request) {
        return oidcServices.get(provider).verifyIdToken(request.id_token(), request.nonce());
    }

    private String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    private User findOrCreateUser(OidcProvider provider, String socialId, String email, String profileNickname) {
        return socialAccountRepository.findByProviderAndSocialId(provider, socialId)
                .map(SocialAccount::getUser)
                .orElseGet(() -> {
                    User newUser = userRepository.save(User.builder()
                            .email(email)
                            .profileNickname(profileNickname)
                            .build());

                    SocialAccount socialAccount = SocialAccount.builder()
                            .user(newUser)
                            .socialId(socialId)
                            .provider(provider)
                            .email(email)
                            .build();

                    socialAccountRepository.save(socialAccount);

                    return newUser;
                });
    }

    private AuthDto.SignInTokenResponse generateSignInResponse(User user) {
        String userId = user.getId().toString();
        String accessToken = jwtProvider.createAccessToken(userId, user.getEmail(), "USER");
        AuthDto.RefreshTokenWithExpiry refresh = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, refresh.token(), refresh.expiry());

        return new AuthDto.SignInTokenResponse(accessToken, refresh.token(), user.getPortalConnected());
    }
}
