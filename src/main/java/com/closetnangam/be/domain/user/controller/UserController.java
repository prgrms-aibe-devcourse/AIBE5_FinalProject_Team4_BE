package com.closetnangam.be.domain.user.controller;

import com.closetnangam.be.domain.user.dto.request.MarketingConsentUpdateRequest;
import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.service.UserService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 userId를 반환합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(userId)));
    }

    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 상세 정보를 반환합니다.")
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long userId) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(userId)));
    }

    @Operation(summary = "마케팅 정보 수신 동의 조회", description = "현재 사용자의 마케팅 정보 수신 동의 상태를 조회합니다.")
    @GetMapping("/{userId}/marketing-consent")
    public ResponseEntity<ApiResponse<MarketingConsentResponse>> getMarketingConsent(@PathVariable Long userId) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(userService.getMarketingConsent(userId)));
    }

    @Operation(summary = "마케팅 정보 수신 동의 변경", description = "온보딩 또는 마이페이지에서 마케팅 정보 수신 동의 여부를 변경합니다.")
    @PatchMapping("/{userId}/marketing-consent")
    public ResponseEntity<ApiResponse<MarketingConsentResponse>> updateMarketingConsent(
            @PathVariable Long userId,
            @Valid @RequestBody MarketingConsentUpdateRequest request
    ) {
        SecurityUtils.verifyUserIdMatch(userId);
        return ResponseEntity.ok(ApiResponse.ok(userService.updateMarketingConsent(userId, request.marketingAgreed())));
    }
}
