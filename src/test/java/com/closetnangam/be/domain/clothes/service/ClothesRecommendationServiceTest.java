package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
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

    @InjectMocks
    private ClothesRecommendationService clothesRecommendationService;

    @Test
    @DisplayName("어울리는 옷 추천은 외부 쇼핑 후보 풀에서 카테고리별 점수 상위를 반환한다")
    void recommendUsesExternalCandidates() {
        WardrobeClothes anchor = createAnchorWardrobeClothes(1L, "TOP", "SHORT_SLEEVE", "WHITE");
        Clothes bottomCandidate = createExternalClothes(200L, "BOTTOM", "SLACKS", "BLACK");
        Clothes ownedDuplicate = createExternalClothes(300L, "BOTTOM", "DENIM", "BLUE");

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(anchor));
        given(wardrobeClothesRepository.findOwnedClothesIdsByUserId(1L, OwnershipStatus.OWNED))
                .willReturn(List.of(10L, 300L));
        given(clothesRepository.findExternalCandidatesForComplementaryRecommendation(eq("TOP"), any(Pageable.class)))
                .willReturn(List.of(bottomCandidate, ownedDuplicate));

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

    private WardrobeClothes createAnchorWardrobeClothes(
            Long userId,
            String category,
            String itemType,
            String color
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

        Clothes clothes = createClothes(10L, category, itemType, color);
        WardrobeClothes wardrobeClothes = WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size("L")
                .season("ALL_SEASON")
                .favorite(false)
                .userImageUrl("https://example.com/anchor.jpg")
                .build();
        ReflectionTestUtils.setField(wardrobeClothes, "id", 20L);
        return wardrobeClothes;
    }

    private Clothes createExternalClothes(Long clothesId, String category, String itemType, String color) {
        Clothes clothes = createClothes(clothesId, category, itemType, color);
        ReflectionTestUtils.setField(clothes, "clothesInfoSource", ClothesInfoSource.EXTERNAL_SHOPPING);
        return clothes;
    }

    private Clothes createClothes(Long clothesId, String category, String itemType, String color) {
        Clothes clothes = Clothes.builder()
                .name("item-" + clothesId)
                .brandName("brand")
                .productCode("CODE-" + clothesId)
                .imageUrl("https://example.com/" + clothesId + ".jpg")
                .category(category)
                .itemType(itemType)
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
