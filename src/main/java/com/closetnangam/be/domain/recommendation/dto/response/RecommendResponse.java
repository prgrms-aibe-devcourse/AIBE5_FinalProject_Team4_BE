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
        String price
) {}