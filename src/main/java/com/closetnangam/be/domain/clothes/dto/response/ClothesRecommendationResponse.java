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
            // TODO: S3 signed URL 도입 시 만료 시간을 포함한 presigned URL로 교체 필요
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
            String brandName,
            String imageUrl,
            // TODO: S3 signed URL 도입 시 만료 시간을 포함한 presigned URL로 교체 필요
            String userImageUrl,
            String category,
            String itemType,
            String primaryColor,
            ColorInfo primaryColorDisplay,
            List<String> secondaryColors,
            List<String> styleCodes,
            String season,
            int compatibilityScore,
            String gender,
            String externalProductUrl
    ) {}

    /** 색상 코드 + 한글명 + hex */
    public record ColorInfo(String code, String label, String hex) {}
}
