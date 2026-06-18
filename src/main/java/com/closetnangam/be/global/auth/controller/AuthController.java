package com.closetnangam.be.global.auth.controller;


import com.closetnangam.be.domain.user.service.UserService;
import com.closetnangam.be.global.auth.jwt.JwtTokenProvider;
import com.closetnangam.be.global.auth.jwt.RefreshTokenService;
import com.closetnangam.be.global.auth.oauth.OAuth2SessionAttributes;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh (
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || !jwtTokenProvider.isValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtTokenProvider.extractUserId(refreshToken);
        String saved = refreshTokenService.get(userId);

        if(!refreshToken.equals(saved)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        ResponseCookie cookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .build();


        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request
    ){
        if(refreshToken != null && jwtTokenProvider.isValid(refreshToken)){
            Long userId = jwtTokenProvider.extractUserId(refreshToken);
            refreshTokenService.delete(userId);
        }

        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }

        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @Operation(
            summary = "탈퇴 계정 복구",
            description = "OAuth 로그인 후 30일 이내 탈퇴 계정으로 확인된 경우, 사용자가 복구를 확정하면 계정을 ACTIVE로 전환하고 인증 쿠키를 발급합니다."
    )
    @PostMapping("/restore-withdrawn")
    public ResponseEntity<ApiResponse<Void>> restoreWithdrawn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("복구 가능한 로그인 세션이 없습니다."));
        }

        Object userIdValue = session.getAttribute(OAuth2SessionAttributes.WITHDRAWN_RESTORE_USER_ID);
        if (!(userIdValue instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("복구 가능한 로그인 세션이 없습니다."));
        }

        userService.restoreWithdrawnUser(userId);
        session.removeAttribute(OAuth2SessionAttributes.WITHDRAWN_RESTORE_USER_ID);

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok(null));
    }
}
