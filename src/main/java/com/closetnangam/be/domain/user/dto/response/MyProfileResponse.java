package com.closetnangam.be.domain.user.dto.response;

import com.closetnangam.be.domain.user.entity.User;

public record MyProfileResponse(
        Long userId,
        String nickname,
        boolean onboarded,
        String regionName,
        User.Gender gender
) {
}
