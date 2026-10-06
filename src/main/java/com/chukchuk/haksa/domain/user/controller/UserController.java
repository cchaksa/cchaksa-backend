package com.chukchuk.haksa.domain.user.controller;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.user.controller.docs.UserControllerDocs;
import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인·내 정보 조회·회원 탈퇴 요청을 사용자 서비스에 위임한다. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController implements UserControllerDocs {

  private final UserService userService;

  @Override
  @GetMapping("/analytics-id")
  public ResponseEntity<SuccessResponse<UserDto.AnalyticsIdResponse>> getAnalyticsId(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UserDto.AnalyticsIdResponse response =
        new UserDto.AnalyticsIdResponse(userDetails.getId().toString());
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @Override
  @GetMapping("/me")
  public ResponseEntity<SuccessResponse<UserDto.MeResponse>> getMe(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UserDto.MeResponse response = userService.getMe(userDetails.getId());
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @Override
  @DeleteMapping("/delete")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> deleteUser(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    long t0 = LogTime.start();
    UUID userId = UUID.fromString(userDetails.getUsername());
    userService.deleteUserById(userId);
    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info("[BIZ] users.delete.done took_ms={}", tookMs);
    }
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("회원 탈퇴가 완료되었습니다.")));
  }

  @Override
  @PostMapping("/signin")
  public ResponseEntity<SuccessResponse<UserDto.SignInResponse>> signInUser(
      @RequestBody UserDto.SignInRequest signInRequest) {
    long t0 = LogTime.start();
    AuthDto.SignInTokenResponse tokens = userService.signIn(signInRequest);
    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info("[BIZ] users.signin.done took_ms={}", tookMs);
    }
    UserDto.SignInResponse response =
        new UserDto.SignInResponse(
            tokens.accessToken(), tokens.refreshToken(), tokens.isPortalLinked());
    return ResponseEntity.ok(SuccessResponse.of(response));
  }
}
