package com.closetnangam.be.domain.purchase.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PurchaseCaptureRegistrationResponse(
        Long wardrobeClothesId,
        Long clothesId,
        Long captureId,
        String userImageUrl,
        OwnershipStatus ownershipStatus,
        @JsonProperty("infoSource") ClothesInfoSource clothesInfoSource,
        String externalSource,
        String size,
        String season,
        Boolean favorite,
        ClothesResponse clothes
) {
}
