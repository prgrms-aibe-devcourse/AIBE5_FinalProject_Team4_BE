package com.closetnangam.be.domain.user.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MyProfileResponse(
        Long userId,
        String nickname,
        boolean onboarded,
        boolean guideTourCompletedHome,
        boolean guideTourCompletedWardrobe,
        boolean guideTourCompletedFeed,
        boolean guideTourCompletedMypage,
        String gender,
        LocalDate birthDate,
        String regionName,
        String regionCode,
        List<String> styles
) {}
