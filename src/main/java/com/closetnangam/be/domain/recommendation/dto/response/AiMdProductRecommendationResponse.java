package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;

import java.util.List;

public record AiMdProductRecommendationResponse(
        AiMdPersonaResponse md,
        String query,
        List<ProductRecommendation> products
) {

    public record ProductRecommendation(
            NaverShoppingProductResponse product,
            String reason
    ) {
    }
}
