package com.chukchuk.haksa.domain.user.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.report.service.ReportLifecycleService;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentDeletionService;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 조회, OIDC 로그인, 계정 병합과 탈퇴를 처리한다. */
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
  private final StudentDeletionService studentDeletionService;
  private final ReportLifecycleService reportLifecycleService;

  private final Map<OidcProvider, OidcService> oidcServices;

  /**
   * 사용자 식별자로 계정을 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 식별자에 해당하는 사용자
   * @throws EntityNotFoundException 식별자에 해당하는 사용자가 없는 경우
   */
  public User getUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  /**
   * 사용자의 포털 연동 완료 여부를 반환한다.
   *
   * @param userId 사용자 식별자
   * @return 사용자의 포털 연동 여부
   */
  public UserDto.MeResponse getMe(UUID userId) {
    User user = getUserById(userId);
    return new UserDto.MeResponse(Boolean.TRUE.equals(user.getPortalConnected()));
  }

  /**
   * 사용자를 저장한다.
   *
   * @param user 연결할 사용자
   */
  @Transactional
  public void save(User user) {
    userRepository.save(user);
  }

  /**
   * OIDC 로그인 요청을 검증하고 인증 토큰을 발급한다.
   *
   * @param signInRequest OIDC 제공자와 ID 토큰을 포함한 로그인 요청
   * @return 로그인 사용자와 발급된 인증 토큰
   */
  @Transactional
  public AuthDto.SignInTokenResponse signIn(UserDto.SignInRequest signInRequest) {
    OidcProvider provider = signInRequest.provider();
    log.info("[BIZ] users.signin.verify.start provider={}", provider);
    Claims claims = verifyToken(provider, signInRequest);
    log.info(
        "[BIZ] users.signin.verify.success provider={} subject={}", provider, claims.getSubject());

    String sub = claims.getSubject();
    String email = extractEmail(claims);

    User user = findOrCreateUser(provider, sub, email, "Unknown User");
    AuthDto.SignInTokenResponse response = generateSignInResponse(user);
    log.info(
        "[BIZ] users.signin.token.issued userId={} portalLinked={}",
        user.getId(),
        user.getPortalConnected());
    return response;
  }

  /**
   * 지정된 데이터를 삭제한다.
   *
   * @param userId 사용자 식별자
   * @throws EntityNotFoundException 삭제할 사용자가 없는 경우
   */
  @Transactional
  public void deleteUserById(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    Student student = user.getStudent();
    UUID studentId = student != null ? student.getId() : null;

    reportLifecycleService.anonymizeByUserId(userId);
    studentDeletionService.anonymizeByStudent(student);
    if (studentId != null) {
      academicCache.deleteAllByStudentId(studentId);
    }
    authTokenCache.evictByUserId(userId.toString());
    socialAccountRepository.deleteByUser(user);
    refreshTokenService.deleteAllByUserId(userId.toString());
    user.withdraw(Instant.now());
    userRepository.save(user);
    log.info("[BIZ] user.withdraw.done userId={}", userId);
  }

  /**
   * 소셜 로그인 후 포털 연동 시, studentCode 기반으로 기존 User가 있는지 탐색하여 병합 시도. - 기존 User가 없다면: currentUser를 그대로 사용
   * - 기존 User가 있다면: - 기존 User의 SocialAccount들을 currentUser에 연결 - 기존 User의 필드값들을 currentUser에 할당 -
   * student에 연결된 기존 User를 currentUser로 변경 - 기존 User 삭제 후 currentUser 리턴
   *
   * @param currentUserId 소셜 로그인으로 인증된 현재 사용자 식별자
   * @param studentCode 기존 사용자와의 중복을 판별할 학번
   * @return 기존 계정을 병합했거나 그대로 유지한 현재 사용자
   */
  @Transactional
  public User tryMergeWithExistingUser(UUID currentUserId, String studentCode) {
    User currentUser = getUserById(currentUserId);
    Optional<User> existingUserOpt = userRepository.findByStudentStudentCode(studentCode);

    if (existingUserOpt.isEmpty()) {
      return currentUser;
    }

    User existingUser = existingUserOpt.get();
    if (existingUser.getId().equals(currentUserId)) {
      log.info("[BIZ] user.merge.skip.self userId={} studentCode={}", currentUserId, studentCode);
      return currentUser;
    }

    // 소셜 계정 모두 이전
    List<SocialAccount> accounts = socialAccountRepository.findAllByUserId(existingUser.getId());
    for (SocialAccount sa : accounts) {
      sa.updateUser(currentUser);
    }

    currentUser.absorbFrom(existingUser);

    Student student = existingUser.getStudent();
    student.updateUser(currentUser);
    existingUser.setStudent(null);

    reportLifecycleService.reassignOwner(existingUser.getId(), currentUserId);
    userRepository.delete(existingUser);
    log.info(
        "[BIZ] user.merged existingUserId={} into currentUserId={}",
        existingUser.getId(),
        currentUserId);

    authTokenCache.evictByUserId(currentUserId.toString());
    return currentUser;
  }

  /* private method */
  private Claims verifyToken(OidcProvider provider, UserDto.SignInRequest request) {
    return oidcServices.get(provider).verifyIdToken(request.idToken(), request.nonce());
  }

  private String extractEmail(Claims claims) {
    return claims.get("email", String.class);
  }

  private User findOrCreateUser(
      OidcProvider provider, String socialId, String email, String profileNickname) {
    return socialAccountRepository
        .findByProviderAndSocialId(provider, socialId)
        .map(
            socialAccount -> {
              log.info(
                  "[BIZ] users.signin.user.found provider={} socialId={} userId={}",
                  provider,
                  socialId,
                  socialAccount.getUser().getId());
              return socialAccount.getUser();
            })
        .orElseGet(
            () -> {
              User newUser =
                  userRepository.save(
                      User.builder().email(email).profileNickname(profileNickname).build());

              SocialAccount socialAccount =
                  SocialAccount.builder()
                      .user(newUser)
                      .socialId(socialId)
                      .provider(provider)
                      .email(email)
                      .build();

              socialAccountRepository.save(socialAccount);
              log.info(
                  "[BIZ] users.signin.user.created provider={} socialId={} userId={}",
                  provider,
                  socialId,
                  newUser.getId());

              return newUser;
            });
  }

  private AuthDto.SignInTokenResponse generateSignInResponse(User user) {
    String userId = user.getId().toString();
    String accessToken = jwtProvider.createAccessToken(userId, user.getEmail(), "USER");
    AuthDto.RefreshTokenWithExpiry refresh = jwtProvider.createRefreshToken(userId);
    refreshTokenService.save(refresh.sessionId(), userId, refresh.token(), refresh.expiry());

    return new AuthDto.SignInTokenResponse(accessToken, refresh.token(), user.getPortalConnected());
  }

  /**
   * 사용자의 인증 상세 정보 캐시를 제거한다.
   *
   * @param userId 사용자 식별자
   */
  public void evictUserDetailsCache(UUID userId) {
    authTokenCache.evictByUserId(userId.toString());
  }
}
