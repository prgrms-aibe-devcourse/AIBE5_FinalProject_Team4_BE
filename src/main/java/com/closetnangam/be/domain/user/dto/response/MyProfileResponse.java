package com.closetnangam.be.domain.user.dto.response;

public record MyProfileResponse(
        Long userId,
        String nickname,
        boolean onboarded,
        boolean guideTourCompletedHome,
        boolean guideTourCompletedWardrobe,
        boolean guideTourCompletedFeed,
        boolean guideTourCompletedMypage
) {}
