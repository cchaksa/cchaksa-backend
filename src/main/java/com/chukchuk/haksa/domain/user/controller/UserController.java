package com.chukchuk.haksa.domain.user.controller;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.user.controller.docs.UserControllerDocs;
import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@LogPart("auth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @DeleteMapping("/delete")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.deleteUserById(userId);
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("회원 탈퇴가 완료되었습니다.")));
    }

    @PostMapping("/signin")
    public ResponseEntity<SuccessResponse<UserDto.SignInResponse>> signInUser(
            @RequestBody UserDto.SignInRequest signInRequest
    ) {
        AuthDto.SignInTokenResponse tokens = userService.signIn(signInRequest);
        UserDto.SignInResponse response = new UserDto.SignInResponse(tokens.accessToken(), tokens.refreshToken(), tokens.isPortalLinked());
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
