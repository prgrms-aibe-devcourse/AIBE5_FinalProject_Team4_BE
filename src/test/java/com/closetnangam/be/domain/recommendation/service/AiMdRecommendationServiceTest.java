package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdGeminiOutfitResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiMdRecommendationServiceTest {

    @Test
    @DisplayName("실제 사용자 옷장에 매핑되는 후보만 저장 가능한 코디로 확정한다")
    void savableOutfitsSkipsCandidatesWithOnlyUnknownWardrobeIds() {
        /*
         * Gemini 응답은 외부 입력이라 존재하지 않는 wardrobeClothesId가 섞일 수 있다.
         * 앞쪽 후보가 raw id만 가지고 있어도 실제 사용자 옷장에 매핑되지 않으면 건너뛰고,
         * 뒤쪽의 저장 가능한 후보까지 확인한 뒤 4개를 확정해야 한다.
         */
        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, null, null, null, null, null
        );
        AiMdGeminiOutfitResult aiResult = new AiMdGeminiOutfitResult(List.of(
                outfit("저장 불가 1", 999L),
                outfit("저장 가능 1", 1L),
                outfit("저장 가능 2", 2L),
                outfit("저장 가능 3", 3L),
                outfit("저장 가능 4", 4L),
                outfit("저장 가능 5", 5L)
        ));
        Map<Long, WardrobeClothes> wardrobeById = Map.of(
                1L, mock(WardrobeClothes.class),
                2L, mock(WardrobeClothes.class),
                3L, mock(WardrobeClothes.class),
                4L, mock(WardrobeClothes.class),
                5L, mock(WardrobeClothes.class)
        );

        List<AiMdGeminiOutfitResult.OutfitCandidate> result = ReflectionTestUtils.invokeMethod(
                service,
                "savableOutfits",
                aiResult,
                wardrobeById
        );

        assertThat(result)
                .extracting(AiMdGeminiOutfitResult.OutfitCandidate::title)
                .containsExactly("저장 가능 1", "저장 가능 2", "저장 가능 3", "저장 가능 4");
    }

    private AiMdGeminiOutfitResult.OutfitCandidate outfit(String title, Long wardrobeClothesId) {
        return new AiMdGeminiOutfitResult.OutfitCandidate(
                title,
                "description",
                "DAILY",
                "ALL_SEASON",
                "reason",
                "tip",
                List.of(wardrobeClothesId),
                List.of()
        );
    }
}
