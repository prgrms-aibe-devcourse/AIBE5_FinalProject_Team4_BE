package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;

public record PhotoClothesRegistrationResponse(
        Long wardrobeClothesId,
        Long clothesId,
        Long photoId,
        String userImageUrl,
        OwnershipStatus ownershipStatus,
        ClothesInfoSource infoSource,
        String size,
        String season,
        Boolean favorite,
        ClothesResponse clothes
) {
}
