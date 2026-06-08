package com.closetnangam.be.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Gemini 상품 추천 응답을 받기 위한 내부 DTO.
 *
 * productId는 네이버 후보 목록에 있는 값만 최종 응답으로 사용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiMdGeminiProductResult(
        List<ProductCandidate> products
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductCandidate(
            String productId,
            String reason
    ) {
    }
}
