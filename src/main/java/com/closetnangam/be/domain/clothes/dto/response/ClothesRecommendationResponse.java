package com.closetnangam.be.domain.clothes.dto.response;

import java.util.List;
import java.util.Map;

public record ClothesRecommendationResponse(
        AnchorItem anchor,
        /** 카테고리 코드 → 추천 목록 (예: "BOTTOM" → [...]) */
        Map<String, List<RecommendedItem>> recommendations
) {

    /** 기준 옷 요약 정보 */
    public record AnchorItem(
            Long clothesId,
            String name,
            String imageUrl,
            String userImageUrl,
            String category,
            String itemType,
            String primaryColor,
            ColorInfo primaryColorDisplay
    ) {}

    /** 추천 옷 1건 */
    public record RecommendedItem(
            Long clothesId,
            Long wardrobeClothesId,
            String name,
            String imageUrl,
            String userImageUrl,
            String category,
            String itemType,
            String primaryColor,
            ColorInfo primaryColorDisplay,
            List<String> secondaryColors,
            List<String> styleCodes,
            String season,
            int compatibilityScore,
            ScoreBreakdown breakdown
    ) {}

    /** 색상 코드 + 한글명 + hex */
    public record ColorInfo(String code, String label, String hex) {}

    /** 점수 구성 (0~100) */
    public record ScoreBreakdown(int colorScore, int styleScore, int seasonScore, int itemTypeScore) {}
}
