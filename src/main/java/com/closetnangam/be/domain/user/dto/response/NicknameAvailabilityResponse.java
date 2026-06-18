package com.closetnangam.be.domain.user.dto.response;

public record NicknameAvailabilityResponse(
        String nickname,
        boolean available,
        String message
) {
}
