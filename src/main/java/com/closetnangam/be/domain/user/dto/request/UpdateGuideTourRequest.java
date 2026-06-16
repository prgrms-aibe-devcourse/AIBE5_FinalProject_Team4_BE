package com.closetnangam.be.domain.user.dto.request;

public record UpdateGuideTourRequest(
        Boolean home,
        Boolean wardrobe,
        Boolean feed,
        Boolean mypage
) {}
