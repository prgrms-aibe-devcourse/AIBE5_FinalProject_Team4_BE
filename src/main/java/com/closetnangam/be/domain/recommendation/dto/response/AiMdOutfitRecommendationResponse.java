package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;

import java.util.List;

public record AiMdOutfitRecommendationResponse(
        AiMdPersonaResponse md,
        List<SavedOutfitRecommendation> outfits
) {

    public record SavedOutfitRecommendation(
            OutfitResponse outfit,
            String reason,
            String stylingTip,
            List<ClothesResponse> ownedItems,
            List<ClothesResponse> externalItems
    ) {
    }
}
