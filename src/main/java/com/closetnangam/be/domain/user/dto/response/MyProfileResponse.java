package com.closetnangam.be.domain.user.dto.response;

import java.time.LocalDate;
import java.util.List;
import com.closetnangam.be.domain.user.entity.User;

public record MyProfileResponse(
        Long userId,
        String email,
        String nickname,
        boolean onboarded,
        boolean guideTourCompletedHome,
        boolean guideTourCompletedWardrobe,
        boolean guideTourCompletedFeed,
        boolean guideTourCompletedMypage,
        boolean guideTourCompletedOutfitBook,
        User.Gender gender,
        LocalDate birthDate,
        String regionName,
        String regionCode,
        String profileImageUrl,
        String profileBio,
        String externalLinkUrl,
        List<String> styleCodes,
        List<String> socialProviders,
        List<SocialAccountResponse> socialAccounts
) {}
