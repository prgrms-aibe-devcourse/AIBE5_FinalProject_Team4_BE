package com.closetnangam.be.domain.user.controller;

import com.closetnangam.be.domain.user.dto.request.UpdateGuideTourRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateProfileRequest;
import com.closetnangam.be.domain.user.dto.request.UpdateStylesRequest;
import com.closetnangam.be.domain.user.dto.request.MarketingConsentUpdateRequest;
import com.closetnangam.be.domain.user.dto.response.MarketingConsentResponse;
import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.service.UserService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 userId, nickname, onboarded를 반환합니다.")
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

    @Operation(summary = "프로필 수정", description = "닉네임, 생년월일, 성별, 지역, 프로필 이미지, 한 줄 소개, 외부 링크를 저장합니다. 온보딩 및 마이페이지에서 공통으로 사용합니다. 저장 후 onboarded 여부를 응답에 포함합니다.")
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        MyProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "가이드 투어 완료 상태 업데이트",
            description = "페이지별 가이드 투어 완료 여부를 업데이트합니다. null인 필드는 기존 값을 유지합니다.")
    @PatchMapping("/guide-tour")
    public ResponseEntity<ApiResponse<Void>> updateGuideTour(
            @RequestBody UpdateGuideTourRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.updateGuideTour(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "스타일 선호도 저장", description = "선택한 스타일 코드 목록을 저장합니다. 기존 UserStyle row를 보존하면서 preference_weight만 갱신합니다. 배열 순서 기준 첫 번째 스타일은 대표(+7), 나머지는 보조(+3), 선택 해제된 스타일은 0으로 낮춥니다. wardrobe_weight, feedback_weight는 유지됩니다.")
    @PostMapping("/styles")
    public ResponseEntity<ApiResponse<Void>> updateStyles(
            @Valid @RequestBody UpdateStylesRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.updateStyles(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 상태를 WITHDRAWN으로 변경하고 인증 쿠키를 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.withdraw(userId);

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
                .body(ApiResponse.ok(null));
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
