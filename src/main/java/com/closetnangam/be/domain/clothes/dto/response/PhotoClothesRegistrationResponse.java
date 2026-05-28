package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.RegistrationSource;

public record PhotoClothesRegistrationResponse(
        Long wardrobeClothesId,
        Long clothesId,
        Long photoId,
        String userImageUrl,
        OwnershipStatus ownershipStatus,
        RegistrationSource registrationSource,
        String size,
        String season,
        Boolean favorite,
        ClothesResponse clothes
) {
}
