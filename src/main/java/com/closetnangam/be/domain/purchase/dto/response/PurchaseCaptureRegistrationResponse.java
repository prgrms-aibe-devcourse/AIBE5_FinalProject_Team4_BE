package com.closetnangam.be.domain.purchase.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;

public record PurchaseCaptureRegistrationResponse(
        Long wardrobeClothesId,
        Long clothesId,
        Long captureId,
        String userImageUrl,
        OwnershipStatus ownershipStatus,
        ClothesInfoSource infoSource,
        String externalSource,
        String size,
        String season,
        Boolean favorite,
        ClothesResponse clothes
) {
}
