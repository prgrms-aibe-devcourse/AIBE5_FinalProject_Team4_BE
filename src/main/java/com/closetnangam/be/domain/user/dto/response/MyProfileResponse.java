package com.closetnangam.be.domain.user.dto.response;

import com.closetnangam.be.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public record MyProfileResponse(
        Long userId,
        String email,
        String nickname,
        boolean onboarded,
        LocalDate birthDate,
        User.Gender gender,
        String regionName,
        String regionCode,
        String profileImageUrl,
        String profileBio,
        String externalLinkUrl,
        List<String> styleCodes,
        List<String> socialProviders,
        List<SocialAccountResponse> socialAccounts
) {
}
