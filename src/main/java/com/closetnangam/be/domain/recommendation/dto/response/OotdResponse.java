package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import lombok.Builder;
import java.util.List;

@Builder
public record OotdResponse(
        List<OotdCombinationResponse> combinations,
        String weatherLabel,
        double currentTemp
) {
    @Builder
    public record OotdCombinationResponse(
            OotdItemResponse top,
            OotdItemResponse bottom,
            OotdItemResponse outer,
            double totalScore
    ) {}

    @Builder
    public record OotdItemResponse(
            Long clothesId,
            Long wardrobeClothesId,
            String name,
            String brand,
            String color,
            String imageUrl,
            String externalProductUrl,
            String category,
            String itemType,
            Boolean favorite
    ) {}
}
