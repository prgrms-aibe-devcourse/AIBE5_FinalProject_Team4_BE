package com.closetnangam.be.domain.user.dto.response;

public record SocialAccountResponse(
        String provider,
        String providerEmail
) {
}
