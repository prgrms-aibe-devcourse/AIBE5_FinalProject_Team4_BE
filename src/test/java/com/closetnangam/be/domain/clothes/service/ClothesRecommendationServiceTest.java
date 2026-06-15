package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.helper.WardrobeExclusionMatcher;
import com.closetnangam.be.domain.clothes.helper.WardrobeExclusionMatcher.WardrobeExclusionIndex;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClothesRecommendationServiceTest {

    @Mock
    private WardrobeClothesRepository wardrobeClothesRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private WardrobeExclusionMatcher wardrobeExclusionMatcher;

    @InjectMocks
    private ClothesRecommendationService clothesRecommendationService;

    @Test
    @DisplayName("어울리는 옷 추천은 DB 전체 후보 풀에서 카테고리별 점수 상위를 반환한다")
    void recommendUsesAllDbCandidatesPerCategory() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "TOP", "SHORT_SLEEVE", "WHITE");
        Clothes bottomCandidate = createExternalClothes(200L, "BOTTOM", "SLACKS", "BLACK");
        Clothes ownedDuplicate = createExternalClothes(300L, "BOTTOM", "DENIM", "BLUE");

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L))
                .willReturn(new WardrobeExclusionIndex(Set.of(10L, 300L), Set.of(), Set.of()));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("BOTTOM"), any(Pageable.class)))
                .willReturn(List.of(bottomCandidate, ownedDuplicate));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations()).containsKey("BOTTOM");
        assertThat(response.recommendations().get("BOTTOM"))
                .hasSize(1)
                .first()
                .satisfies(item -> {
                    assertThat(item.clothesId()).isEqualTo(200L);
                    assertThat(item.wardrobeClothesId()).isNull();
                    assertThat(item.season()).isEqualTo("ALL_SEASON");
                });
    }

    @Test
    @DisplayName("기준 옷 시즌이 여름이면 겨울 아우터(패딩)는 추천에서 제외한다")
    void recommendExcludesWinterOuterWhenAnchorSeasonIsSummer() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "BOTTOM", "SHORTS", "WHITE", "SUMMER");
        Clothes padding = createExternalClothes(200L, "OUTER", "PADDING", "BLACK");
        Clothes windbreaker = createExternalClothes(201L, "OUTER", "WINDBREAKER", "NAVY");

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L))
                .willReturn(new WardrobeExclusionIndex(Set.of(10L), Set.of(), Set.of()));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("TOP"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of(padding, windbreaker));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations().get("OUTER"))
                .extracting(item -> item.clothesId())
                .containsExactly(201L);
    }

    @Test
    @DisplayName("기준 옷 시즌이 ALL_SEASON이면 겨울 아우터도 점수 기준으로 추천될 수 있다")
    void recommendDoesNotHardExcludeWinterOuterWhenAnchorSeasonIsAllSeason() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "BOTTOM", "SHORTS", "WHITE", "ALL_SEASON");
        Clothes padding = createExternalClothes(200L, "OUTER", "PADDING", "BLACK");

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L))
                .willReturn(new WardrobeExclusionIndex(Set.of(10L), Set.of(), Set.of()));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("TOP"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of(padding));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations().get("OUTER"))
                .extracting(item -> item.clothesId())
                .containsExactly(200L);
    }

    @Test
    @DisplayName("외부 후보에 WINTER 시즌이 저장되어 있으면 여름 기준 옷 추천에서 제외한다")
    void recommendExcludesWinterOuterWhenCandidateSeasonIsWinter() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "BOTTOM", "SHORTS", "WHITE", "SUMMER");
        Clothes padding = createExternalClothes(200L, "OUTER", "PADDING", "BLACK", ClothesSeason.WINTER);

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L))
                .willReturn(new WardrobeExclusionIndex(Set.of(10L), Set.of(), Set.of()));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("TOP"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of(padding));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations()).doesNotContainKey("OUTER");
    }

    @Test
    @DisplayName("어울림 점수가 같으면 brand_name이 UNKNOWN이 아닌 상품을 우선 추천한다")
    void recommendPrefersKnownBrandWhenCompatibilityScoreIsTied() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "TOP", "SHORT_SLEEVE", "WHITE");
        Clothes unknownBrandBottom = createExternalClothes(200L, "BOTTOM", "SLACKS", "BLACK");
        ReflectionTestUtils.setField(unknownBrandBottom, "brandName", "UNKNOWN");
        Clothes knownBrandBottom = createExternalClothes(201L, "BOTTOM", "SLACKS", "BLACK");
        ReflectionTestUtils.setField(knownBrandBottom, "brandName", "폴햄");

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L))
                .willReturn(new WardrobeExclusionIndex(Set.of(10L), Set.of(), Set.of()));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("BOTTOM"), any(Pageable.class)))
                .willReturn(List.of(unknownBrandBottom, knownBrandBottom));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations().get("BOTTOM"))
                .extracting(item -> item.clothesId())
                .containsExactly(201L, 200L);
        assertThat(response.recommendations().get("BOTTOM"))
                .extracting(item -> item.compatibilityScore())
                .containsOnly(84);
    }

    @Test
    @DisplayName("보유 전환으로 복제된 옷과 동일 identity의 원본 외부 상품은 추천에서 제외한다")
    void recommendExcludesOriginalExternalProductWhenOwnedCloneHasSameIdentity() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "TOP", "SHORT_SLEEVE", "WHITE");
        Clothes ownedClone = createExternalClothes(99L, "BOTTOM", "SLACKS", "BLACK");
        ReflectionTestUtils.setField(ownedClone, "name", "slacks");
        ReflectionTestUtils.setField(ownedClone, "clothesInfoSource", ClothesInfoSource.PURCHASE_HISTORY);
        ReflectionTestUtils.setField(ownedClone, "externalProductId", Clothes.EXTERNAL_NONE);
        Clothes originalExternal = createExternalClothes(200L, "BOTTOM", "SLACKS", "BLACK");
        ReflectionTestUtils.setField(originalExternal, "name", "slacks");
        ReflectionTestUtils.setField(originalExternal, "externalProductId", "ext-200");
        ReflectionTestUtils.setField(originalExternal, "externalSource", "MUSINSA");

        WardrobeClothes ownedEntry = createOwnedLink(ownedClone);
        WardrobeExclusionIndex exclusionIndex = WardrobeExclusionIndex.fromActiveWardrobeEntries(List.of(ownedEntry));

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeExclusionMatcher.buildActiveExclusionIndex(1L)).willReturn(exclusionIndex);
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("BOTTOM"), any(Pageable.class)))
                .willReturn(List.of(originalExternal));
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("OUTER"), any(Pageable.class)))
                .willReturn(List.of());
        given(clothesRepository.findComplementaryRecommendationCandidatesByCategory(eq("SHOES"), any(Pageable.class)))
                .willReturn(List.of());

        ClothesRecommendationResponse response = clothesRecommendationService.recommend(1L, 10L, 5);

        assertThat(response.recommendations()).doesNotContainKey("BOTTOM");
    }

    private WardrobeClothes createOwnedLink(Clothes clothes) {
        User user = User.builder()
                .nickname("user-1")
                .email("user1@example.com")
                .gender(User.Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Wardrobe wardrobe = Wardrobe.create(user);
        ReflectionTestUtils.setField(wardrobe, "id", 100L);

        WardrobeClothes wardrobeClothes = WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size("L")
                .favorite(false)
                .userImageUrl("https://example.com/user.jpg")
                .build();
        ReflectionTestUtils.setField(wardrobeClothes, "id", 21L);
        return wardrobeClothes;
    }

    private WardrobeClothes createAnchorWardrobeClothes(
            Long userId,
            String category,
            String itemType,
            String color
    ) {
        return createAnchorWardrobeClothes(userId, category, itemType, color, "ALL_SEASON");
    }

    private WardrobeClothes createAnchorWardrobeClothes(
            Long userId,
            String category,
            String itemType,
            String color,
            String season
    ) {
        User user = User.builder()
                .nickname("user-" + userId)
                .email("user" + userId + "@example.com")
                .gender(User.Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wardrobe wardrobe = Wardrobe.create(user);
        ReflectionTestUtils.setField(wardrobe, "id", 100L);

        Clothes clothes = createClothes(10L, category, itemType, color, ClothesSeason.fromCodeOrDefault(season));
        WardrobeClothes wardrobeClothes = WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size("L")
                .favorite(false)
                .userImageUrl("https://example.com/anchor.jpg")
                .build();
        ReflectionTestUtils.setField(wardrobeClothes, "id", 20L);
        return wardrobeClothes;
    }

    private Clothes createExternalClothes(Long clothesId, String category, String itemType, String color) {
        return createExternalClothes(clothesId, category, itemType, color, ClothesSeason.ALL_SEASON);
    }

    private Clothes createExternalClothes(
            Long clothesId,
            String category,
            String itemType,
            String color,
            ClothesSeason season
    ) {
        Clothes clothes = createClothes(clothesId, category, itemType, color, season);
        ReflectionTestUtils.setField(clothes, "clothesInfoSource", ClothesInfoSource.EXTERNAL_SHOPPING);
        return clothes;
    }

    private Clothes createClothes(Long clothesId, String category, String itemType, String color) {
        return createClothes(clothesId, category, itemType, color, ClothesSeason.ALL_SEASON);
    }

    private Clothes createClothes(Long clothesId, String category, String itemType, String color, ClothesSeason season) {
        Clothes clothes = Clothes.builder()
                .name("item-" + clothesId)
                .brandName("brand")
                .productCode("CODE-" + clothesId)
                .imageUrl("https://example.com/" + clothesId + ".jpg")
                .category(category)
                .itemType(itemType)
                .season(season)
                .clothesInfoSource(ClothesInfoSource.PHOTO)
                .externalSource("NONE")
                .externalProductId("NONE")
                .externalProductUrl("NONE")
                .isVerified(false)
                .build();
        ReflectionTestUtils.setField(clothes, "id", clothesId);

        clothes.addColorTag(ClothingColor.create(clothes, color, ColorRole.PRIMARY, (byte) 1));
        Style style = Style.from(StyleCode.CASUAL);
        ReflectionTestUtils.setField(style, "id", 1L);
        clothes.addStyleTag(ClothesStyleTag.create(clothes, style, StyleRole.PRIMARY, (byte) 1));
        return clothes;
    }
}
