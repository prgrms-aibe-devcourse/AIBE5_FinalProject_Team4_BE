package com.closetnangam.be.global.common.controller;

import com.closetnangam.be.global.auth.dev.DevUserService;
import com.closetnangam.be.global.auth.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Mock Auth", description = "개발용 임시 인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@Profile("local")
public class MockAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final DevUserService devUserService;

    public MockAuthController(JwtTokenProvider jwtTokenProvider, DevUserService devUserService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.devUserService = devUserService;
    }

    @Operation(summary = "테스트용 JWT 토큰 발급", description = "로그인 없이 고유 ID를 지정해 임시 액세스 토큰을 생성합니다.")
    @GetMapping("/mock-token")
    public ResponseEntity<Map<String, Object>> getMockToken(
            @RequestParam(defaultValue = "1") Long userId
    ) {
        devUserService.ensureDevUser(userId);

        String accessToken = jwtTokenProvider.createAccessToken(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "임시 토큰이 정상 발급되었습니다. 개발용으로만 사용하세요.");
        response.put("data", Map.of(
                "userId", userId,
                "accessToken", accessToken,
                "headerValue", "Bearer " + accessToken
        ));

        return ResponseEntity.ok(response);
    }
}