package com.closetnangam.be.domain.recommendation.dto.response;

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

        @Schema(description = "스타일")
        String primaryStyle,

        @Schema(description = "옷 ID (피드백용)")
        Long clothesId
) {}