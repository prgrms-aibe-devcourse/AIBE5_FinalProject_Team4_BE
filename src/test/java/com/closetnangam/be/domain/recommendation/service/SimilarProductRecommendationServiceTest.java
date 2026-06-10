package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.recommendation.dto.response.SimilarProductRecommendationResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.global.external.naver.service.NaverApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimilarProductRecommendationServiceTest {

    @Mock
    private WardrobeClothesRepository wardrobeClothesRepository;

    @Mock
    private NaverApiService naverApiService;

    @InjectMocks
    private SimilarProductRecommendationService similarProductRecommendationService;

    @Test
    @DisplayName("브랜드를 제외하고 성별, 색상, 디자인 키워드, 스타일, 타입, 카테고리로 유사상품 검색어를 만든다")
    void recommendSimilarProductsBuildsAttributeBasedQuery() {
        /*
         * 이 테스트는 네이버 API를 실제 호출하지 않고, 서비스가 어떤 검색어를 만드는지만 검증한다.
         * 추천 품질의 핵심이 검색어 조합 규칙이므로 외부 API 응답보다 이 규칙을 안정적으로 고정하는 것이 중요하다.
         */
        WardrobeClothes wardrobeClothes = createWardrobeClothes(
                1L,
                User.Gender.MALE,
                "ourselves",
                "multi stripe long sleeve",
                "BLACK",
                "LONG_SLEEVE",
                "TOP",
                StyleCode.CASUAL
        );

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.of(wardrobeClothes));
        given(naverApiService.searchShoppingProducts(anyString())).willReturn(List.of());

        SimilarProductRecommendationResponse response =
                similarProductRecommendationService.recommendSimilarProducts(1L, 10L);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(naverApiService).searchShoppingProducts(queryCaptor.capture());

        assertThat(queryCaptor.getValue()).isEqualTo("남성 블랙 스트라이프 캐주얼 롱슬리브");
        assertThat(response.query()).isEqualTo("남성 블랙 스트라이프 캐주얼 롱슬리브");
        assertThat(response.baseClothes().brandName()).isEqualTo("ourselves");
    }

    @Test
    @DisplayName("선택한 옷이 요청 사용자의 옷이 아니면 추천을 차단한다")
    void recommendSimilarProductsRejectsOtherUsersClothes() {
        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> similarProductRecommendationService.recommendSimilarProducts(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 사용자의 옷을 찾을 수 없습니다.");
    }

    private WardrobeClothes createWardrobeClothes(
            Long userId,
            User.Gender gender,
            String brandName,
            String name,
            String color,
            String itemType,
            String category,
            StyleCode styleCode
    ) {
        /*
         * 엔티티 생성자와 연관관계는 실제 도메인 모델을 그대로 사용한다.
         * 최신 모델에서는 사용자 소유 정보가 Clothes가 아니라 WardrobeClothes에 있으므로,
         * 테스트도 WardrobeClothes를 기준으로 fixture를 만든다.
         */
        User user = User.builder()
                .nickname("test-user-" + userId)
                .email("test" + userId + "@example.com")
                .gender(gender)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wardrobe wardrobe = Wardrobe.create(user);
        ReflectionTestUtils.setField(wardrobe, "id", 100L + userId);

        Clothes clothes = Clothes.builder()
                .name(name)
                .brandName(brandName)
                .productCode("NONE")
                .imageUrl("https://example.com/image.jpg")
                .category(category)
                .itemType(itemType)
                .clothesInfoSource(ClothesInfoSource.PHOTO)
                .externalSource("NONE")
                .externalProductId("NONE")
                .externalProductUrl("NONE")
                .isVerified(true)
                .build();
        ReflectionTestUtils.setField(clothes, "id", 10L);

        clothes.addColorTag(ClothingColor.create(clothes, color, ColorRole.PRIMARY, (byte) 1));

        Style style = Style.from(styleCode);
        ReflectionTestUtils.setField(style, "id", 1L);
        clothes.addStyleTag(ClothesStyleTag.create(clothes, style, StyleRole.PRIMARY, (byte) 1));

        WardrobeClothes wardrobeClothes = WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size("L")
                .favorite(false)
                .userImageUrl("https://example.com/user-image.jpg")
                .build();
        ReflectionTestUtils.setField(wardrobeClothes, "id", 20L);

        return wardrobeClothes;
    }
}
