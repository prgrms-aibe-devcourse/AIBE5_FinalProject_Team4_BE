package com.closetnangam.be.global.common.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new AccessDeniedException("인증 정보가 없습니다.");
    }

    public static void verifyOwnership(Long pathUserId) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(pathUserId)) {
            throw new AccessDeniedException("다른 사용자의 리소스에 접근할 수 없습니다.");
        }
    }
}
