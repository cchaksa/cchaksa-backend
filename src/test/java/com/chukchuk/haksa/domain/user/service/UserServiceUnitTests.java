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
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTests {

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
    private OidcService oidcService;

    @Test
    @DisplayName("userId로 사용자를 조회할 수 있다")
    void getUserById_success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("found@example.com")
                .profileNickname("found")
                .build();

        UserService userService = createService();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User found = userService.getUserById(userId);

        assertThat(found).isSameAs(user);
    }

    @Test
    @DisplayName("userId에 해당하는 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void getUserById_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UserService userService = createService();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("studentCode에 해당하는 기존 사용자가 없으면 현재 사용자를 그대로 반환한다")
    void tryMergeWithExistingUser_whenNoExistingUser_returnsCurrentUser() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder()
                .id(currentUserId)
                .email("current@example.com")
                .profileNickname("current")
                .build();

        UserService userService = createService();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByStudent_StudentCode("20201234")).thenReturn(Optional.empty());

        User merged = userService.tryMergeWithExistingUser(currentUserId, "20201234");

        assertThat(merged).isSameAs(currentUser);
        verify(userRepository, never()).delete(any(User.class));
        verify(authTokenCache, never()).evictByUserId(anyString());
    }

    @Test
    @DisplayName("기존 사용자 발견 시 social account와 student 연관관계를 현재 사용자로 병합하고 기존 사용자를 삭제한다")
    void tryMergeWithExistingUser_whenExistingUserExists_mergesAndDeletesExisting() {
        UUID currentUserId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(currentUserId)
                .email("current@example.com")
                .profileNickname("current")
                .build();

        User existingUser = User.builder()
                .id(existingUserId)
                .email("existing@example.com")
                .profileNickname("existing")
                .build();
        existingUser.markPortalConnected(Instant.now());

        Student existingStudent = org.mockito.Mockito.mock(Student.class);
        existingUser.setStudent(existingStudent);

        SocialAccount first = SocialAccount.builder()
                .provider(OidcProvider.KAKAO)
                .socialId("social-id-1")
                .email("existing@example.com")
                .user(existingUser)
                .build();
        SocialAccount second = SocialAccount.builder()
                .provider(OidcProvider.KAKAO)
                .socialId("social-id-2")
                .email("existing2@example.com")
                .user(existingUser)
                .build();

        UserService userService = createService();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByStudent_StudentCode("20201234")).thenReturn(Optional.of(existingUser));
        when(socialAccountRepository.findAllByUserId(existingUserId)).thenReturn(List.of(first, second));

        User merged = userService.tryMergeWithExistingUser(currentUserId, "20201234");

        assertThat(merged).isSameAs(currentUser);
        assertThat(first.getUser()).isSameAs(currentUser);
        assertThat(second.getUser()).isSameAs(currentUser);
        assertThat(currentUser.getEmail()).isEqualTo("existing@example.com");
        assertThat(currentUser.getPortalConnected()).isTrue();
        verify(existingStudent).updateUser(currentUser);
        verify(userRepository).delete(existingUser);
        verify(authTokenCache).evictByUserId(currentUserId.toString());
    }

    @Test
    @DisplayName("병합 대상 current user가 없으면 USER_NOT_FOUND 예외를 던진다")
    void tryMergeWithExistingUser_whenCurrentMissing_throws() {
        UUID currentUserId = UUID.randomUUID();
        UserService userService = createService();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.tryMergeWithExistingUser(currentUserId, "20201234"))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("회원 탈퇴 시 학생 캐시/토큰/소셜계정을 정리한 뒤 사용자를 삭제한다")
    void deleteUserById_cleansUpAndDeletesUser() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("delete@example.com")
                .profileNickname("delete")
                .build();

        Student student = org.mockito.Mockito.mock(Student.class);
        when(student.getId()).thenReturn(studentId);
        user.setStudent(student);

        UserService userService = createService();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUserById(userId);

        verify(academicCache).deleteAllByStudentId(studentId);
        verify(authTokenCache).evictByUserId(userId.toString());
        verify(socialAccountRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("회원 탈퇴 대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void deleteUserById_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        UserService userService = createService();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("소셜 계정이 이미 존재하면 기존 사용자로 로그인 토큰을 발급한다")
    void signIn_whenSocialAccountExists_returnsTokensForExistingUser() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .profileNickname("existing")
                .build();

        SocialAccount existingAccount = SocialAccount.builder()
                .provider(OidcProvider.KAKAO)
                .socialId("social-sub")
                .email("existing@example.com")
                .user(existingUser)
                .build();

        Claims claims = Jwts.claims();
        claims.setSubject("social-sub");
        claims.put("email", "existing@example.com");

        UserService userService = createService();
        when(oidcService.verifyIdToken("id-token", "nonce")).thenReturn(claims);
        when(socialAccountRepository.findByProviderAndSocialId(OidcProvider.KAKAO, "social-sub"))
                .thenReturn(Optional.of(existingAccount));
        when(jwtProvider.createAccessToken(existingUser.getId().toString(), "existing@example.com", "USER"))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(existingUser.getId().toString()))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("refresh-token", new Date()));

        AuthDto.SignInTokenResponse response = userService.signIn(new UserDto.SignInRequest("id-token", "nonce"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).save(eq(existingUser.getId().toString()), eq("refresh-token"), any(Date.class));
        verify(userRepository, never()).save(any(User.class));
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
    }

    @Test
    @DisplayName("소셜 계정이 없으면 사용자와 소셜 계정을 새로 생성하고 로그인 토큰을 발급한다")
    void signIn_whenSocialAccountMissing_createsUserAndSocialAccount() {
        UUID newUserId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(newUserId)
                .email("new@example.com")
                .profileNickname("Unknown User")
                .build();

        Claims claims = Jwts.claims();
        claims.setSubject("new-social-sub");
        claims.put("email", "new@example.com");

        UserService userService = createService();
        when(oidcService.verifyIdToken("id-token", "nonce")).thenReturn(claims);
        when(socialAccountRepository.findByProviderAndSocialId(OidcProvider.KAKAO, "new-social-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.createAccessToken(savedUser.getId().toString(), "new@example.com", "USER"))
                .thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(savedUser.getId().toString()))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("new-refresh-token", new Date()));

        AuthDto.SignInTokenResponse response = userService.signIn(new UserDto.SignInRequest("id-token", "nonce"));

        ArgumentCaptor<SocialAccount> socialCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(socialCaptor.capture());

        SocialAccount createdSocial = socialCaptor.getValue();
        assertThat(createdSocial.getProvider()).isEqualTo(OidcProvider.KAKAO);
        assertThat(createdSocial.getSocialId()).isEqualTo("new-social-sub");
        assertThat(createdSocial.getEmail()).isEqualTo("new@example.com");
        assertThat(createdSocial.getUser()).isSameAs(savedUser);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.isPortalLinked()).isFalse();
        verify(refreshTokenService).save(eq(savedUser.getId().toString()), eq("new-refresh-token"), any(Date.class));
    }

    @Test
    @DisplayName("evictUserDetailsCache는 auth token 캐시 제거를 위임한다")
    void evictUserDetailsCache_delegatesToAuthTokenCache() {
        UUID userId = UUID.randomUUID();
        UserService userService = createService();

        userService.evictUserDetailsCache(userId);

        verify(authTokenCache).evictByUserId(userId.toString());
    }

    @Test
    @DisplayName("save는 userRepository에 위임한다")
    void save_delegatesToRepository() {
        User user = User.builder()
                .email("save@example.com")
                .profileNickname("save")
                .build();

        UserService userService = createService();
        userService.save(user);

        verify(userRepository).save(user);
    }

    private UserService createService() {
        return new UserService(
                userRepository,
                socialAccountRepository,
                jwtProvider,
                refreshTokenService,
                academicCache,
                authTokenCache,
                Map.of(OidcProvider.KAKAO, oidcService)
        );
    }
}
