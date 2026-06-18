package com.closetnangam.be.domain.recommendation.dto.response;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 추천 응답 DTO")
public record RecommendResponse(

        @Schema(description = "상품명")
        String title,

        @Schema(description = "상품 링크")
        String link,

        @Schema(description = "이미지 URL")
        String imageUrl,

        @Schema(description = "가격")
        String price,

        @Schema(description = "추천 점수")
        String score,

        @Schema(description = "추천 이유")
        String reason,

        @Schema(description = "브랜드명")
        String brandName,

        @Schema(description = "카테고리")
        String category,

        @Schema(description = "대표 색상")
        String primaryColor,

        @Schema(description = "대표 색상 표시 정보")
        ClothesResponse.ColorDisplayResponse primaryColorDisplay,

        @Schema(description = "스타일")
        String primaryStyle,

        @Schema(description = "옷 ID (피드백용)")
        Long clothesId
) {
        public static ClothesResponse.ColorDisplayResponse toColorDisplay(String colorCode) {
                if (colorCode == null) return null;
                try {
                        ClothesColor clothesColor = ClothesColor.fromCode(colorCode);
                        return new ClothesResponse.ColorDisplayResponse(
                                clothesColor.name(),
                                clothesColor.getLabel(),
                                clothesColor.getHex()
                        );
                } catch (Exception e) {
                        return null;
                }
        }
}