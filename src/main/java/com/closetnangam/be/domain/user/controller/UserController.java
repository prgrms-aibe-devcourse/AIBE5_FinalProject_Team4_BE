package com.closetnangam.be.domain.user.controller;

import com.closetnangam.be.domain.user.dto.response.MyProfileResponse;
import com.closetnangam.be.domain.user.dto.response.UserProfileResponse;
import com.closetnangam.be.domain.user.service.UserService;
import com.closetnangam.be.global.auth.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<MyProfileResponse> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 상세 정보를 반환합니다.")
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }
}