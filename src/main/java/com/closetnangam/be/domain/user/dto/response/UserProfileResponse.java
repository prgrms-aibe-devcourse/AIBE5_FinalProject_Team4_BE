package com.closetnangam.be.domain.user.dto.response;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String profileBio,
        String externalLinkUrl
) {
}