package com.closetnangam.be.domain.recommendation.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiMdGeminiOutfitResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Gemini가 외부 상품 목록을 생략해도 빈 목록으로 정규화한다")
    void normalizesMissingExternalProductIdsToEmptyList() throws Exception {
        /*
         * AI MD 코디는 보유 옷만으로도 유효하다.
         * Gemini가 optional 필드인 externalProductIds를 생략하면 저장 서비스에서 stream() 호출 시 NPE가 날 수 있으므로
         * DTO 역직렬화 단계에서 빈 리스트 계약을 고정한다.
         */
        String json = """
                {
                  "outfits": [
                    {
                      "title": "보유 옷 단독 코디",
                      "description": "외부 상품 없이 구성한 코디",
                      "wardrobeClothesIds": [1]
                    }
                  ]
                }
                """;

        AiMdGeminiOutfitResult result = objectMapper.readValue(json, AiMdGeminiOutfitResult.class);

        AiMdGeminiOutfitResult.OutfitCandidate outfit = result.outfits().get(0);
        assertThat(outfit.wardrobeClothesIds()).containsExactly(1L);
        assertThat(outfit.externalProductIds()).isEmpty();
    }

    @Test
    @DisplayName("Gemini가 null 목록을 반환해도 빈 목록으로 정규화한다")
    void normalizesNullListsToEmptyLists() throws Exception {
        String json = """
                {
                  "outfits": [
                    {
                      "title": "목록 null 코디",
                      "wardrobeClothesIds": null,
                      "externalProductIds": null
                    }
                  ]
                }
                """;

        AiMdGeminiOutfitResult result = objectMapper.readValue(json, AiMdGeminiOutfitResult.class);

        AiMdGeminiOutfitResult.OutfitCandidate outfit = result.outfits().get(0);
        assertThat(outfit.wardrobeClothesIds()).isEmpty();
        assertThat(outfit.externalProductIds()).isEmpty();
    }
}
