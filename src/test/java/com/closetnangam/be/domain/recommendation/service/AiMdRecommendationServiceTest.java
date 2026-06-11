package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdGeminiOutfitResult;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                1L, wardrobeItem("TOP"),
                2L, wardrobeItem("TOP"),
                3L, wardrobeItem("TOP"),
                4L, wardrobeItem("TOP"),
                5L, wardrobeItem("TOP")
        );
        Map<String, NaverShoppingProductResponse> productById = Map.of(
                "bottom-1", product("bottom-1", "스트릿 카고 팬츠", "바지"),
                "shoes-1", product("shoes-1", "스트릿 스니커즈", "운동화")
        );

        List<AiMdGeminiOutfitResult.OutfitCandidate> result = ReflectionTestUtils.invokeMethod(
                service,
                "savableOutfits",
                aiResult,
                wardrobeById,
                productById
        );

        assertThat(result)
                .extracting(AiMdGeminiOutfitResult.OutfitCandidate::title)
                .containsExactly("저장 가능 1", "저장 가능 2", "저장 가능 3", "저장 가능 4");
    }

    @Test
    @DisplayName("상의만 조합한 후보는 완성형 코디에서 제외한다")
    void savableOutfitsRejectsOutfitWithoutBottomAndShoes() {
        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, null, null, null, null, null
        );
        AiMdGeminiOutfitResult aiResult = new AiMdGeminiOutfitResult(List.of(
                new AiMdGeminiOutfitResult.OutfitCandidate(
                        "상의 레이어드뿐인 코디",
                        "description",
                        "DAILY",
                        "ALL_SEASON",
                        "reason",
                        "tip",
                        List.of(1L),
                        List.of("top-1")
                )
        ));

        List<AiMdGeminiOutfitResult.OutfitCandidate> result = ReflectionTestUtils.invokeMethod(
                service,
                "savableOutfits",
                aiResult,
                Map.of(1L, wardrobeItem("TOP")),
                Map.of("top-1", product("top-1", "레이어드 반팔 티셔츠", "티셔츠"))
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("코디 프롬프트는 실제 MD처럼 구체적인 추천 사유를 작성하도록 요구한다")
    void outfitPromptRequiresNaturalPersonaReason() {
        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, null, null, null, null, null
        );

        String prompt = ReflectionTestUtils.invokeMethod(
                service,
                "buildOutfitPrompt",
                AiMdPersona.TAE_SIK,
                List.of(),
                List.of()
        );

        assertThat(prompt)
                .contains("추천 사유 화법:")
                .contains("왜 이 코디가 나에게 어울리는지")
                .contains("TOP(상의), BOTTOM(하의), SHOES(신발)를 각각 최소 1개")
                .contains("상의만 여러 개 조합한 결과는 코디로 인정하지 않습니다")
                .contains("전체 분량은 한글 기준 약 180~260자")
                .contains("선택한 보유 옷과 외부 상품을 빠짐없이 한 번씩 언급")
                .contains("상의·하의·아우터·신발 등 각 아이템이 코디에서 맡는 역할")
                .contains("아이템별 설명을 따로 나열하지 말고")
                .contains("태식이 MD가 사용자에게 직접 코디를 제안하는 말투")
                .contains("\"AI\", \"인공지능\", \"모델\", \"데이터\", \"분석 결과\", \"알고리즘\"")
                .contains("사용자의 키, 체중, 체형, 신체 비율은 제공되지 않았으므로")
                .contains("상품명에 체형을 지칭하는 표현이 포함되어 있어도 추천 사유에는 옮겨 쓰지 않습니다")
                .contains("처음부터 끝까지 존댓말 없이")
                .contains("자연스러운 반말")
                .contains("가볍게 장난")
                .contains("사용자를 놀리거나 무례하게 말하지 않고");
    }

    @Test
    @DisplayName("추천 사유 fallback도 MD별 말투를 유지한다")
    void defaultOutfitReasonReflectsPersonaVoice() {
        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, null, null, null, null, null
        );

        String taeSikReason = ReflectionTestUtils.invokeMethod(
                service,
                "defaultOutfitReason",
                AiMdPersona.TAE_SIK
        );
        String junSikReason = ReflectionTestUtils.invokeMethod(
                service,
                "defaultOutfitReason",
                AiMdPersona.JUN_SIK
        );
        String seSoonReason = ReflectionTestUtils.invokeMethod(
                service,
                "defaultOutfitReason",
                AiMdPersona.SE_SOON
        );
        String gaHyunReason = ReflectionTestUtils.invokeMethod(
                service,
                "defaultOutfitReason",
                AiMdPersona.GA_HYUN
        );
        String seongMiReason = ReflectionTestUtils.invokeMethod(
                service,
                "defaultOutfitReason",
                AiMdPersona.SEONG_MI
        );

        assertThat(taeSikReason).contains("옷 좀 입었다는 소리").endsWith("듣겠는데?");
        assertThat(junSikReason).contains("도시적인 인상").endsWith("조합입니다.");
        assertThat(seSoonReason).contains("깔끔하게").endsWith("조합입니다.");
        assertThat(gaHyunReason).contains("도회적인 무드").endsWith("조합이에요.");
        assertThat(seongMiReason).contains("편하게 활용").endsWith("조합이에요.");
        assertThat(Set.of(taeSikReason, junSikReason, seSoonReason, gaHyunReason, seongMiReason))
                .hasSize(AiMdPersona.values().length);
    }

    @Test
    @DisplayName("모든 MD는 서로 다른 추천 사유 화법 지침을 가진다")
    void everyPersonaHasDistinctRecommendationVoiceGuide() {
        assertThat(List.of(AiMdPersona.values()))
                .extracting(AiMdPersona::recommendationVoiceGuide)
                .allMatch(guide -> guide != null && !guide.isBlank())
                .doesNotHaveDuplicates();
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
                List.of("bottom-1", "shoes-1")
        );
    }

    private WardrobeClothes wardrobeItem(String category) {
        Clothes clothes = mock(Clothes.class);
        when(clothes.getCategory()).thenReturn(category);
        WardrobeClothes wardrobeClothes = mock(WardrobeClothes.class);
        when(wardrobeClothes.getClothes()).thenReturn(clothes);
        return wardrobeClothes;
    }

    private NaverShoppingProductResponse product(
            String productId,
            String title,
            String category3
    ) {
        return new NaverShoppingProductResponse(
                title,
                "https://example.com/" + productId,
                "https://example.com/" + productId + ".jpg",
                10000,
                null,
                "테스트몰",
                productId,
                "1",
                "테스트브랜드",
                "테스트제조사",
                "패션의류",
                "남성의류",
                category3,
                ""
        );
    }
}
