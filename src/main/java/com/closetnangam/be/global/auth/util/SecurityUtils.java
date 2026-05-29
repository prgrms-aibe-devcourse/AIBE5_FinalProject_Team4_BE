package com.closetnangam.be.global.auth.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 현재 인증된 사용자의 userId를 반환합니다.
     * JwtAuthenticationFilter가 principal을 Long(userId)로 설정합니다.
     *
     * @throws IllegalStateException 인증 컨텍스트가 없거나 principal 타입이 맞지 않을 경우
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
        }
        return userId;
    }

    /**
     * path variable의 userId가 토큰의 userId와 다르면 403을 유발하는 예외를 던집니다.
     */
    public static void verifyUserIdMatch(Long pathUserId) {
        if (!getCurrentUserId().equals(pathUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("접근 권한이 없습니다.");
        }
    }
}
