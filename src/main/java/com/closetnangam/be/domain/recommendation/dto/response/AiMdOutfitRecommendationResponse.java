package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;

import java.util.List;

public record AiMdOutfitRecommendationResponse(
        AiMdPersonaResponse md,
        List<OutfitRecommendation> outfits
) {

    /**
     * 아직 저장하지 않은 AI MD 코디 후보.
     *
     * 프론트는 이 후보를 카드로 보여준 뒤, 사용자가 저장 버튼을 누른 후보만 저장 API body로 전달한다.
     */
    public record OutfitRecommendation(
            String title,
            String description,
            String situation,
            String season,
            String reason,
            String stylingTip,
            List<ClothesResponse> ownedItems,
            List<NaverShoppingProductResponse> externalProducts
    ) {
    }

    public record SavedOutfitRecommendation(
            OutfitResponse outfit,
            String reason,
            String stylingTip,
            List<ClothesResponse> ownedItems,
            List<ClothesResponse> externalItems
    ) {
    }
}
