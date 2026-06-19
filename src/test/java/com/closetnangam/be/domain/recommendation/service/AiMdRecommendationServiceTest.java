package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.outfit.service.OutfitStyleService;
import com.closetnangam.be.domain.recommendation.dto.request.AiMdOutfitSaveRequest;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdGeminiOutfitResult;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdProductRecommendationResponse.ProductRecommendation;
import com.closetnangam.be.domain.recommendation.repository.RecommendationFeedbackRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
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
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
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
    @DisplayName("옷장 등록 옷 없이 외부 후보만으로도 완성형 코디를 확정할 수 있다")
    void savableOutfitsAllowExternalOnlyCompleteOutfit() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
        AiMdGeminiOutfitResult aiResult = new AiMdGeminiOutfitResult(List.of(
                new AiMdGeminiOutfitResult.OutfitCandidate(
                        "외부 후보 풀착장",
                        "description",
                        "DAILY",
                        "ALL_SEASON",
                        "reason",
                        "tip",
                        List.of(),
                        List.of("top-1", "bottom-1", "shoes-1")
                )
        ));

        List<AiMdGeminiOutfitResult.OutfitCandidate> result = ReflectionTestUtils.invokeMethod(
                service,
                "savableOutfits",
                aiResult,
                Map.of(),
                Map.of(
                        "top-1", product("top-1", "스트릿 반팔 티셔츠", "티셔츠"),
                        "bottom-1", product("bottom-1", "와이드 카고 팬츠", "바지"),
                        "shoes-1", product("shoes-1", "블랙 스니커즈", "운동화")
                )
        );

        assertThat(result)
                .extracting(AiMdGeminiOutfitResult.OutfitCandidate::title)
                .containsExactly("외부 후보 풀착장");
    }

    @Test
    @DisplayName("AI MD 코디 후보 검증은 내부 후보의 DB 카테고리 코드를 우선 사용한다")
    void savableOutfitsUseInternalProductCategoryCode() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
        AiMdGeminiOutfitResult aiResult = new AiMdGeminiOutfitResult(List.of(
                new AiMdGeminiOutfitResult.OutfitCandidate(
                        "내부 후보 신발 코디",
                        "description",
                        "DAILY",
                        "ALL_SEASON",
                        "reason",
                        "tip",
                        List.of(1L, 2L),
                        List.of("CLOTHES_100")
                )
        ));
        NaverShoppingProductResponse internalShoes = new NaverShoppingProductResponse(
                "Plain Product",
                "",
                "",
                null,
                null,
                "",
                "CLOTHES_100",
                "INTERNAL",
                "",
                "",
                "패션의류",
                "남성의류",
                "SHOES",
                "스니커즈",
                100L,
                "INTERNAL"
        );

        List<AiMdGeminiOutfitResult.OutfitCandidate> result = ReflectionTestUtils.invokeMethod(
                service,
                "savableOutfits",
                aiResult,
                Map.of(1L, wardrobeItem("TOP"), 2L, wardrobeItem("BOTTOM")),
                Map.of("CLOTHES_100", internalShoes)
        );

        assertThat(result)
                .extracting(AiMdGeminiOutfitResult.OutfitCandidate::title)
                .containsExactly("내부 후보 신발 코디");
    }

    @Test
    @DisplayName("저장 요청 검증은 내부 후보 DTO가 아니라 실제 Clothes 카테고리를 기준으로 한다")
    void saveValidationUsesResolvedInternalClothesCategory() {
        ClothesRepository clothesRepository = mock(ClothesRepository.class);
        Clothes resolvedInternalClothes = mock(Clothes.class);
        when(resolvedInternalClothes.getClothesInfoSource()).thenReturn(ClothesInfoSource.EXTERNAL_SHOPPING);
        when(resolvedInternalClothes.getCategory()).thenReturn("TOP");
        when(clothesRepository.findById(100L)).thenReturn(Optional.of(resolvedInternalClothes));

        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, clothesRepository, null, null, null, null, null, null, null
        );
        NaverShoppingProductResponse forgedProduct = new NaverShoppingProductResponse(
                "스니커즈처럼 조작한 상의",
                "",
                "",
                null,
                null,
                "",
                "CLOTHES_100",
                "INTERNAL",
                "",
                "",
                "패션의류",
                "남성의류",
                "운동화",
                "스니커즈",
                100L,
                "INTERNAL"
        );

        boolean result = ReflectionTestUtils.invokeMethod(
                service,
                "hasCompleteSaveOutfitComposition",
                List.of(wardrobeItem("TOP"), wardrobeItem("BOTTOM")),
                List.of(forgedProduct)
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("코디 프롬프트는 실제 MD처럼 구체적인 추천 사유를 작성하도록 요구한다")
    void outfitPromptRequiresNaturalPersonaReason() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);

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
                .contains("wardrobeClothesIds는 비어 있어도 됩니다")
                .contains("\"wardrobeClothesIds\": []")
                .contains("룩 전체의 조화, 색상 연결, 실루엣 균형, 소재감, 상황 적합성")
                .contains("후보 목록에서 매번 서로 다른 중심 아이템을 랜덤하게 고르되")
                .contains("상의만 여러 개 조합한 결과는 코디로 인정하지 않습니다")
                .contains("ownershipStatus가 OWNED인 보유 옷과 WISHLIST인 미보유 관심 상품")
                .contains("전체 분량은 한글 기준 약 180~260자")
                .contains("선택한 옷장 등록 옷이 있다면 빠짐없이 언급")
                .contains("상의·하의·아우터·신발 등 각 아이템이 코디에서 맡는 역할")
                .contains("아이템별 설명을 따로 나열하지 말고")
                .contains("같은 옷장 등록 옷 조합, 같은 외부 상품 조합, 같은 코디 제목과 사유가 반복되지 않도록")
                .contains("신발 중심, 하의 중심, 아우터 포인트, 상의 레이어드")
                .contains("태식이 MD가 사용자에게 직접 코디를 제안하는 말투")
                .contains("태식이 MD라면 네 필드 모두 존댓말 없이 반말")
                .contains("\"입니다\", \"습니다\", \"해요\", \"이에요\", \"주세요\"")
                .contains("딱딱한 \"~다\" 평서형으로 끝내지 말고")
                .contains("\"~야\", \"~해\", \"~좋아\", \"~어울려\"")
                .contains("\"AI\", \"인공지능\", \"모델\", \"데이터\", \"분석 결과\", \"알고리즘\"")
                .contains("사용자의 키, 체중, 체형, 신체 비율은 제공되지 않았으므로")
                .contains("상품명에 체형을 지칭하는 표현이 포함되어 있어도 추천 사유에는 옮겨 쓰지 않습니다")
                .contains("처음부터 끝까지 존댓말 없이")
                .contains("자연스러운 반말")
                .contains("가볍게 장난")
                .contains("사용자를 놀리거나 무례하게 말하지 않고");
    }

    @Test
    @DisplayName("AI MD 코디 후보에는 보유 옷과 미보유 관심 상품을 함께 사용한다")
    void aiMdWardrobeItemsIncludeOwnedAndWishlist() {
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);
        WardrobeClothes wardrobeItem = wardrobeItem("TOP");
        when(wardrobeClothesRepository.findAllActiveByUserIdAndOwnershipStatuses(
                1L,
                List.of(OwnershipStatus.OWNED, OwnershipStatus.WISHLIST)
        )).thenReturn(List.of(wardrobeItem));

        AiMdRecommendationService service = new AiMdRecommendationService(
                null, wardrobeClothesRepository, null, null, null, null, null, null, null, null, null, null, null
        );

        List<?> result = ReflectionTestUtils.invokeMethod(service, "findAiMdWardrobeItems", 1L);

        assertThat(result).hasSize(1);
        verify(wardrobeClothesRepository).findAllActiveByUserIdAndOwnershipStatuses(
                1L,
                List.of(OwnershipStatus.OWNED, OwnershipStatus.WISHLIST)
        );
    }

    @Test
    @DisplayName("AI MD 코디 저장은 공용 외부 쇼핑 후보가 아닌 clothesId를 외부 상품으로 연결하지 않는다")
    void getOrCreateExternalClothesRejectsNonExternalShoppingClothesId() {
        ClothesRepository clothesRepository = mock(ClothesRepository.class);
        Clothes privateClothes = mock(Clothes.class);
        when(privateClothes.getClothesInfoSource()).thenReturn(ClothesInfoSource.PHOTO);
        when(clothesRepository.findById(999L)).thenReturn(Optional.of(privateClothes));

        AiMdRecommendationService service = new AiMdRecommendationService(
                null, null, null, null, null, clothesRepository, null, null, null, null, null, null, null
        );
        NaverShoppingProductResponse forgedProduct = new NaverShoppingProductResponse(
                "개인 옷",
                "",
                "",
                null,
                null,
                "",
                "CLOTHES_999",
                "INTERNAL",
                "",
                "",
                "패션의류",
                "남성의류",
                "TOP",
                "티셔츠",
                999L,
                "INTERNAL"
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service,
                "getOrCreateExternalClothes",
                AiMdPersona.TAE_SIK,
                forgedProduct
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공용 외부 쇼핑 후보만 외부 상품으로 저장할 수 있습니다.");
    }

    @Test
    @DisplayName("태식이 코디 문구는 서버에서 한 번 더 반말 톤으로 보정한다")
    void taesikOutfitTextToneRemovesPoliteEndings() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);

        String result = ReflectionTestUtils.invokeMethod(
                service,
                "applyOutfitTextTone",
                AiMdPersona.TAE_SIK,
                "상의가 중심을 잡아 좋습니다. 하의와 신발도 잘 어울립니다. 그대로 입기 좋은 조합이에요. 전체적으로 좋은 조합이다."
        );

        assertThat(result)
                .contains("좋아")
                .contains("어울려")
                .contains("조합이야")
                .doesNotContain("좋습니다")
                .doesNotContain("어울립니다")
                .doesNotContain("이에요")
                .doesNotContain("조합이다");
    }

    @Test
    @DisplayName("추천 사유 fallback도 MD별 말투를 유지한다")
    void defaultOutfitReasonReflectsPersonaVoice() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);

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

    @Test
    @DisplayName("사용자 스타일 점수와 MD 친화도를 상품 검색 가중치에 반영한다")
    void productSearchProfilesReflectUserStyleWeights() {
        UserStyleRepository userStyleRepository = mock(UserStyleRepository.class);
        UserStyle street = userStyle("STREET", "스트릿", 20);
        UserStyle minimal = userStyle("MINIMAL", "미니멀", 5);
        when(userStyleRepository.findAllByUserId(1L)).thenReturn(List.of(minimal, street));

        AiMdRecommendationService service = serviceWithUserStyleRepository(userStyleRepository);
        List<?> profiles = ReflectionTestUtils.invokeMethod(
                service,
                "buildStyleSearchProfiles",
                1L,
                AiMdPersona.TAE_SIK
        );

        assertThat(profiles).hasSize(2);
        Object first = profiles.get(0);
        Object second = profiles.get(1);
        String firstName = ReflectionTestUtils.invokeMethod(first, "name");
        Integer firstWeight = ReflectionTestUtils.invokeMethod(first, "weight");
        String secondName = ReflectionTestUtils.invokeMethod(second, "name");
        Integer secondWeight = ReflectionTestUtils.invokeMethod(second, "weight");

        assertThat(firstName).isEqualTo("스트릿");
        assertThat(firstWeight).isEqualTo(23);
        assertThat(secondName).isEqualTo("미니멀");
        assertThat(secondWeight).isEqualTo(5);
    }

    @Test
    @DisplayName("상품명이 같으면 네이버 productId가 달라도 동일 상품 후보로 판별한다")
    void productIdentityKeyDeduplicatesSameNormalizedTitle() {
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
        NaverShoppingProductResponse first = product("product-1", "나이키 에어 반팔 티셔츠", "티셔츠");
        NaverShoppingProductResponse second = product("product-2", "나이키  에어-반팔 티셔츠", "티셔츠");

        String firstKey = ReflectionTestUtils.invokeMethod(service, "productIdentityKey", first);
        String secondKey = ReflectionTestUtils.invokeMethod(service, "productIdentityKey", second);

        assertThat(firstKey).isEqualTo(secondKey);
    }

    @Test
    @DisplayName("상품 추천 프롬프트는 스타일 가중치와 브랜드 및 카테고리 다양성을 요구한다")
    void productPromptRequiresWeightedDiversity() {
        UserStyleRepository userStyleRepository = mock(UserStyleRepository.class);
        when(userStyleRepository.findAllByUserId(1L)).thenReturn(List.of());
        AiMdRecommendationService service = serviceWithUserStyleRepository(userStyleRepository);
        List<?> profiles = ReflectionTestUtils.invokeMethod(
                service,
                "buildStyleSearchProfiles",
                1L,
                AiMdPersona.TAE_SIK
        );

        String prompt = ReflectionTestUtils.invokeMethod(
                service,
                "buildProductPrompt",
                AiMdPersona.TAE_SIK,
                List.of(),
                profiles,
                List.of()
        );

        assertThat(prompt)
                .contains("추천 상품 40개")
                .contains("products 배열은 가능한 한 40개")
                .contains("[사용자 스타일 가중치]")
                .contains("source가 INTERNAL인 상품")
                .contains("INTERNAL 상품을 우선 추천")
                .contains("가중치가 높은 스타일의 상품은 더 자주")
                .contains("낮은 양수 스타일도 일부 섞어")
                .contains("같은 상품, 이름만 조금 다른 동일 모델")
                .contains("브랜드가 한 종류에 치우치지 않도록");
    }

    @Test
    @DisplayName("AI MD 상품 내부 후보는 MD 성별과 유니섹스 상품만 조회한다")
    void aiMdProductInternalCandidatesUsePersonaGenderAndUnisex() {
        ClothesRepository clothesRepository = mock(ClothesRepository.class);
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);
        RecommendationFeedbackRepository recommendationFeedbackRepository = mock(RecommendationFeedbackRepository.class);
        when(wardrobeClothesRepository.findOwnedClothesIdsByUserId(1L, OwnershipStatus.OWNED)).thenReturn(List.of());
        when(wardrobeClothesRepository.findOwnedClothesIdsByUserId(1L, OwnershipStatus.WISHLIST)).thenReturn(List.of());
        when(recommendationFeedbackRepository.findAllByUserId(1L)).thenReturn(List.of());

        AiMdRecommendationService service = new AiMdRecommendationService(
                null, wardrobeClothesRepository, null, null, null,
                clothesRepository, recommendationFeedbackRepository,
                null, null, null, null, null, null
        );
        when(clothesRepository.findExternalShoppingRecommendationCandidates(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(
                service,
                "searchProductCandidates",
                1L,
                AiMdPersona.GA_HYUN,
                List.of(),
                List.of(),
                List.of()
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClothesGender>> genderCaptor = ArgumentCaptor.forClass(List.class);
        verify(clothesRepository).findExternalShoppingRecommendationCandidates(
                anyList(),
                genderCaptor.capture(),
                any(Pageable.class)
        );
        assertThat(genderCaptor.getValue()).containsExactly(ClothesGender.FEMALE, ClothesGender.UNISEX);
    }

    @Test
    @DisplayName("AI MD 상품 후보는 내부 상품을 우선하고 MD 스타일 적합도가 높은 상품을 앞에 둔다")
    void aiMdProductCandidatesPreferInternalProductsByPersonaFit() {
        ClothesRepository clothesRepository = mock(ClothesRepository.class);
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);
        RecommendationFeedbackRepository recommendationFeedbackRepository = mock(RecommendationFeedbackRepository.class);
        when(wardrobeClothesRepository.findOwnedClothesIdsByUserId(1L, OwnershipStatus.OWNED)).thenReturn(List.of());
        when(wardrobeClothesRepository.findOwnedClothesIdsByUserId(1L, OwnershipStatus.WISHLIST)).thenReturn(List.of());
        when(recommendationFeedbackRepository.findAllByUserId(1L)).thenReturn(List.of());

        Clothes weakCandidate = externalClothes(
                101L,
                "미니멀 셔츠",
                "WHITE",
                "MINIMAL",
                "TOP",
                "SHIRT"
        );
        Clothes strongCandidate = externalClothes(
                102L,
                "스트릿 블랙 롱슬리브",
                "BLACK",
                "STREET",
                "TOP",
                "LONG_SLEEVE"
        );
        when(clothesRepository.findExternalShoppingRecommendationCandidates(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(List.of(weakCandidate, strongCandidate));

        AiMdRecommendationService service = new AiMdRecommendationService(
                null, wardrobeClothesRepository, null, null, null,
                clothesRepository, recommendationFeedbackRepository,
                null, null, null, null, null, null
        );
        WardrobeClothes wardrobeItem = wardrobeItemWithClothes(externalClothes(
                201L,
                "내 블랙 티셔츠",
                "BLACK",
                "CASUAL",
                "TOP",
                "SHORT_SLEEVE"
        ));
        UserStyleRepository userStyleRepository = mock(UserStyleRepository.class);
        when(userStyleRepository.findAllByUserId(1L)).thenReturn(List.of(userStyle("STREET", "스트릿", 20)));
        List<?> styleProfiles = ReflectionTestUtils.invokeMethod(
                serviceWithUserStyleRepository(userStyleRepository),
                "buildStyleSearchProfiles",
                1L,
                AiMdPersona.TAE_SIK
        );

        @SuppressWarnings("unchecked")
        List<NaverShoppingProductResponse> result = ReflectionTestUtils.invokeMethod(
                service,
                "searchProductCandidates",
                1L,
                AiMdPersona.TAE_SIK,
                List.of(wardrobeItem),
                styleProfiles,
                List.of()
        );

        assertThat(result)
                .extracting(NaverShoppingProductResponse::clothesId)
                .containsExactly(102L, 101L);
        assertThat(result)
                .allMatch(product -> "INTERNAL".equals(product.candidateSource()));
    }

    @Test
    @DisplayName("상품 추천 1차 선별은 같은 브랜드와 카테고리의 개수를 제한한다")
    void productRecommendationLimitsBrandAndCategoryConcentration() {
        /*
         * Gemini가 한 브랜드나 상의만 반복 선택하더라도 최종 API 응답이 그대로 쏠리지 않아야 한다.
         * 브랜드는 최대 2개, 카테고리는 최대 4개까지만 1차 추천 목록에 포함한다.
         */
        AiMdRecommendationService service = serviceWithUserStyleRepository(null);
        List<ProductRecommendation> recommendations = new ArrayList<>();
        Set<String> selectedProductKeys = new HashSet<>();
        Map<String, Integer> brandCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("nike-1", "나이키 반팔 1", "티셔츠", "나이키")
        )).isTrue();
        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("nike-2", "나이키 반팔 2", "티셔츠", "나이키")
        )).isTrue();
        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("nike-3", "나이키 반팔 3", "티셔츠", "나이키")
        )).isFalse();

        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("brand-a", "브랜드A 반팔", "티셔츠", "브랜드A")
        )).isTrue();
        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("brand-b", "브랜드B 반팔", "티셔츠", "브랜드B")
        )).isTrue();
        assertThat(addRecommendation(
                service, recommendations, selectedProductKeys, brandCounts, categoryCounts,
                product("brand-c", "브랜드C 반팔", "티셔츠", "브랜드C")
        )).isFalse();

        assertThat(recommendations).hasSize(4);
        assertThat(brandCounts).containsEntry("나이키", 2);
        assertThat(categoryCounts).containsEntry("TOP", 4);
    }

    private boolean addRecommendation(
            AiMdRecommendationService service,
            List<ProductRecommendation> recommendations,
            Set<String> selectedProductKeys,
            Map<String, Integer> brandCounts,
            Map<String, Integer> categoryCounts,
            NaverShoppingProductResponse product
    ) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                service,
                "addRecommendationIfDiverse",
                recommendations,
                selectedProductKeys,
                brandCounts,
                categoryCounts,
                new ProductRecommendation(product, "추천 사유")
        ));
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

    private WardrobeClothes wardrobeItemWithClothes(Clothes clothes) {
        WardrobeClothes wardrobeClothes = mock(WardrobeClothes.class);
        when(wardrobeClothes.getClothes()).thenReturn(clothes);
        return wardrobeClothes;
    }

    private Clothes externalClothes(
            Long id,
            String name,
            String colorCode,
            String styleCode,
            String category,
            String itemType
    ) {
        Clothes clothes = Clothes.builder()
                .name(name)
                .brandName("테스트브랜드")
                .productCode("PRODUCT-" + id)
                .imageUrl("https://example.com/" + id + ".jpg")
                .category(category)
                .itemType(itemType)
                .gender(ClothesGender.MALE)
                .season(ClothesSeason.ALL_SEASON)
                .clothesInfoSource(ClothesInfoSource.EXTERNAL_SHOPPING)
                .externalSource("NAVER")
                .externalProductId("EXTERNAL-" + id)
                .externalProductUrl("https://example.com/products/" + id)
                .isVerified(true)
                .build();
        ReflectionTestUtils.setField(clothes, "id", id);
        clothes.addColorTag(ClothingColor.create(clothes, colorCode, ColorRole.PRIMARY, (byte) 1));
        Style style = Style.builder()
                .code(styleCode)
                .name(styleCode)
                .description(styleCode + " 스타일")
                .build();
        ReflectionTestUtils.setField(style, "id", id);
        clothes.addStyleTag(ClothesStyleTag.create(clothes, style, StyleRole.PRIMARY, (byte) 1));
        return clothes;
    }

    private UserStyle userStyle(String code, String name, int combinedWeight) {
        User user = mock(User.class);
        Style style = Style.builder()
                .code(code)
                .name(name)
                .description(name + " 스타일")
                .build();
        UserStyle userStyle = UserStyle.builder()
                .user(user)
                .style(style)
                .build();
        ReflectionTestUtils.setField(userStyle, "combinedWeight", combinedWeight);
        return userStyle;
    }

    private AiMdRecommendationService serviceWithUserStyleRepository(UserStyleRepository userStyleRepository) {
        return new AiMdRecommendationService(
                null, null, null, null, null, null, null, null, userStyleRepository, null, null, null, null
        );  // 마지막 null이 OutfitStyleService
    }

    private NaverShoppingProductResponse product(
            String productId,
            String title,
            String category3
    ) {
        return product(productId, title, category3, "테스트브랜드");
    }

    private NaverShoppingProductResponse product(
            String productId,
            String title,
            String category3,
            String brand
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
                brand,
                "테스트제조사",
                "패션의류",
                "남성의류",
                category3,
                ""
        );
    }

    private NaverShoppingProductResponse internalProduct(
            Long clothesId,
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
                "INTERNAL",
                productId,
                "INTERNAL",
                "테스트브랜드",
                "테스트제조사",
                "패션의류",
                "남성의류",
                category3,
                "",
                clothesId,
                "INTERNAL"
        );
    }

    private Clothes externalShoppingClothes(String category) {
        Clothes clothes = mock(Clothes.class);
        when(clothes.getClothesInfoSource()).thenReturn(ClothesInfoSource.EXTERNAL_SHOPPING);
        when(clothes.getCategory()).thenReturn(category);
        when(clothes.getSortedColorTags()).thenReturn(List.of());
        when(clothes.getSortedStyleTags()).thenReturn(List.of());
        return clothes;
    }

    @Test
    @DisplayName("AI MD 코디 저장 시 outfitStyleService.saveOutfitStyles가 호출된다")
    void saveRecommendedOutfit_호출시_outfitStyles_저장된다() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);
        OutfitBookRepository outfitBookRepository = mock(OutfitBookRepository.class);
        OutfitRepository outfitRepository = mock(OutfitRepository.class);
        OutfitItemRepository outfitItemRepository = mock(OutfitItemRepository.class);
        OutfitStyleService outfitStyleService = mock(OutfitStyleService.class);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getGender()).thenReturn(User.Gender.MALE);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        Clothes clothes = mock(Clothes.class);
        when(clothes.getCategory()).thenReturn("TOP");
        when(clothes.getSortedStyleTags()).thenReturn(List.of());

        WardrobeClothes wardrobeClothes = mock(WardrobeClothes.class);
        when(wardrobeClothes.getId()).thenReturn(1L);
        when(wardrobeClothes.getClothes()).thenReturn(clothes);
        when(wardrobeClothes.getUserImageUrl()).thenReturn("https://image.url");

        Clothes bottomClothes = mock(Clothes.class);
        when(bottomClothes.getCategory()).thenReturn("BOTTOM");
        when(bottomClothes.getSortedStyleTags()).thenReturn(List.of());
        WardrobeClothes bottomWardrobe = mock(WardrobeClothes.class);
        when(bottomWardrobe.getId()).thenReturn(2L);
        when(bottomWardrobe.getClothes()).thenReturn(bottomClothes);

        Clothes shoesClothes = mock(Clothes.class);
        when(shoesClothes.getCategory()).thenReturn("SHOES");
        when(shoesClothes.getSortedStyleTags()).thenReturn(List.of());
        WardrobeClothes shoesWardrobe = mock(WardrobeClothes.class);
        when(shoesWardrobe.getId()).thenReturn(3L);
        when(shoesWardrobe.getClothes()).thenReturn(shoesClothes);

        when(wardrobeClothesRepository.findOwnedForStatistics(1L, com.closetnangam.be.domain.clothes.enums.OwnershipStatus.OWNED))
                .thenReturn(List.of(wardrobeClothes, bottomWardrobe, shoesWardrobe));

        OutfitBook outfitBook = mock(OutfitBook.class);
        when(outfitBook.getId()).thenReturn(1L);
        when(outfitBookRepository.findByUser_Id(1L)).thenReturn(java.util.Optional.of(outfitBook));

        Outfit outfit = mock(Outfit.class);
        when(outfit.getOutfitBook()).thenReturn(outfitBook);
        when(outfit.getOutfitId()).thenReturn(1L);
        when(outfitRepository.save(any())).thenReturn(outfit);

        OutfitItem savedItem = mock(OutfitItem.class);
        when(savedItem.getClothes()).thenReturn(clothes);
        when(outfitItemRepository.saveAll(any())).thenReturn(List.of(savedItem));

        AiMdRecommendationService service = new AiMdRecommendationService(
                userRepository, wardrobeClothesRepository, outfitBookRepository,
                outfitRepository, outfitItemRepository,
                null, null, null, null, null, null, null,
                outfitStyleService
        );

        AiMdOutfitSaveRequest request = new AiMdOutfitSaveRequest(
                "테스트 코디", "설명", "DAILY", "ALL_SEASON",
                "reason", "tip",
                List.of(1L, 2L, 3L),
                List.of()
        );

        // when
        ReflectionTestUtils.invokeMethod(
                service, "saveOutfitRecommendation",
                AiMdPersona.TAE_SIK, outfitBook,
                "테스트 코디", "설명", "DAILY", "ALL_SEASON",
                "reason", "tip",
                List.of(wardrobeClothes, bottomWardrobe, shoesWardrobe),
                List.of()
        );

        // then
        verify(outfitStyleService, times(1)).saveOutfitStyles(any(), anyList());
    }

    @Test
    @DisplayName("AI MD 코디 저장은 옷장 등록 옷 없이 외부 후보만으로 완성형 코디를 저장할 수 있다")
    void saveRecommendedOutfitAllowsExternalOnlyCompleteOutfit() {
        UserRepository userRepository = mock(UserRepository.class);
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);
        OutfitBookRepository outfitBookRepository = mock(OutfitBookRepository.class);
        OutfitRepository outfitRepository = mock(OutfitRepository.class);
        OutfitItemRepository outfitItemRepository = mock(OutfitItemRepository.class);
        ClothesRepository clothesRepository = mock(ClothesRepository.class);
        OutfitStyleService outfitStyleService = mock(OutfitStyleService.class);

        User user = mock(User.class);
        when(user.getGender()).thenReturn(User.Gender.MALE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(wardrobeClothesRepository.findAllActiveByUserIdAndOwnershipStatuses(
                1L,
                List.of(OwnershipStatus.OWNED, OwnershipStatus.WISHLIST)
        )).thenReturn(List.of());

        OutfitBook outfitBook = mock(OutfitBook.class);
        when(outfitBook.getId()).thenReturn(1L);
        when(outfitBookRepository.findByUser_Id(1L)).thenReturn(Optional.of(outfitBook));

        Outfit outfit = mock(Outfit.class);
        when(outfit.getOutfitBook()).thenReturn(outfitBook);
        when(outfit.getOutfitId()).thenReturn(10L);
        when(outfitRepository.save(any())).thenReturn(outfit);
        when(outfitItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes top = externalShoppingClothes("TOP");
        Clothes bottom = externalShoppingClothes("BOTTOM");
        Clothes shoes = externalShoppingClothes("SHOES");
        when(clothesRepository.findById(101L)).thenReturn(Optional.of(top));
        when(clothesRepository.findById(102L)).thenReturn(Optional.of(bottom));
        when(clothesRepository.findById(103L)).thenReturn(Optional.of(shoes));

        AiMdRecommendationService service = new AiMdRecommendationService(
                userRepository, wardrobeClothesRepository, outfitBookRepository,
                outfitRepository, outfitItemRepository,
                clothesRepository, null, null, null, null, null, null,
                outfitStyleService
        );
        AiMdOutfitSaveRequest request = new AiMdOutfitSaveRequest(
                "외부 후보 코디", "설명", "DAILY", "ALL_SEASON",
                "reason", "tip",
                List.of(),
                List.of(
                        internalProduct(101L, "CLOTHES_101", "추천 상의", "TOP"),
                        internalProduct(102L, "CLOTHES_102", "추천 하의", "BOTTOM"),
                        internalProduct(103L, "CLOTHES_103", "추천 신발", "SHOES")
                )
        );

        ReflectionTestUtils.invokeMethod(service, "saveRecommendedOutfit", 1L, "taesik", request);

        verify(outfitItemRepository).saveAll(any());
        verify(outfitStyleService).saveOutfitStyles(any(), anyList());
    }

    @Test
    @DisplayName("AI MD 코디 저장은 현재 사용자 옷장에 없는 wardrobeClothesId를 허용하지 않는다")
    void saveRecommendedOutfitRejectsUnknownWardrobeClothesId() {
        UserRepository userRepository = mock(UserRepository.class);
        WardrobeClothesRepository wardrobeClothesRepository = mock(WardrobeClothesRepository.class);

        User user = mock(User.class);
        when(user.getGender()).thenReturn(User.Gender.MALE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(wardrobeClothesRepository.findAllActiveByUserIdAndOwnershipStatuses(
                1L,
                List.of(OwnershipStatus.OWNED, OwnershipStatus.WISHLIST)
        )).thenReturn(List.of());

        AiMdRecommendationService service = new AiMdRecommendationService(
                userRepository, wardrobeClothesRepository, null,
                null, null, null, null, null, null, null, null, null,
                null
        );
        AiMdOutfitSaveRequest request = new AiMdOutfitSaveRequest(
                "외부 후보 코디", "설명", "DAILY", "ALL_SEASON",
                "reason", "tip",
                List.of(999L),
                List.of()
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "saveRecommendedOutfit", 1L, "taesik", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 사용자의 옷장 등록 옷만 저장할 수 있습니다.");
    }
}
