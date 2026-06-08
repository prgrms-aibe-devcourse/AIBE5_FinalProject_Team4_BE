package com.closetnangam.be.domain.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Gemini 코디 구성 응답을 받기 위한 내부 DTO.
 *
 * 서비스에서 실제 존재하는 wardrobeClothesId/productId만 검증한 뒤 저장 DTO로 변환한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiMdGeminiOutfitResult(
        List<OutfitCandidate> outfits
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutfitCandidate(
            String title,
            String description,
            String situation,
            String season,
            String reason,
            String stylingTip,
            List<Long> wardrobeClothesIds,
            List<String> externalProductIds
    ) {
        public OutfitCandidate {
            /*
             * Gemini는 선택 필드를 생략하거나 null로 반환할 수 있다.
             * 코디는 보유 옷만으로도 유효하므로, 외부 상품 목록은 서비스 저장 단계에서 빈 컬렉션처럼 다루도록 정규화한다.
             */
            wardrobeClothesIds = wardrobeClothesIds == null ? List.of() : wardrobeClothesIds;
            externalProductIds = externalProductIds == null ? List.of() : externalProductIds;
        }
    }
}
