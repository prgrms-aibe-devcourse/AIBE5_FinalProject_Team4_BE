package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.clothes.dto.request.ClothesConvertToOwnedRequest;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClothesServiceTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private WardrobeClothesRepository wardrobeClothesRepository;

    @Mock
    private ClothesTagHelper clothesTagHelper;

    @Mock
    private WardrobeService wardrobeService;

    @InjectMocks
    private ClothesService clothesService;

    @Test
    @DisplayName("기존 CLOTHES 위시리스트 연결은 EXTERNAL_SHOPPING만 허용한다")
    void addExistingClothesToWishlist_rejectsNonExternalShoppingMaster() {
        Clothes privateClothes = createClothes(10L, ClothesInfoSource.PHOTO);
        given(clothesRepository.findById(10L)).willReturn(Optional.of(privateClothes));

        assertThatThrownBy(() -> clothesService.addExistingClothesToWishlist(1L, 10L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("옷을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("외부 추천 상품 보유 전환 시 공용 CLOTHES 마스터는 유지하고 사용자 전용 행을 복제한다")
    void convertToOwned_clonesExternalShoppingMasterWithoutMutatingSharedRow() {
        Clothes sharedExternal = createClothes(10L, ClothesInfoSource.EXTERNAL_SHOPPING);
        WardrobeClothes wardrobeClothes = createWishlistLink(sharedExternal);

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L))
                .willReturn(Optional.of(wardrobeClothes));
        given(clothesRepository.save(any(Clothes.class))).willAnswer(invocation -> {
            Clothes saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });

        ClothesConvertToOwnedRequest request = new ClothesConvertToOwnedRequest(
                "PRODUCT-001",
                "L",
                "https://example.com/user.jpg",
                true
        );

        var response = clothesService.convertToOwned(1L, 10L, request);

        assertThat(sharedExternal.getClothesInfoSource()).isEqualTo(ClothesInfoSource.EXTERNAL_SHOPPING);
        assertThat(sharedExternal.getExternalSource()).isEqualTo("MUSINSA");
        assertThat(wardrobeClothes.getOwnershipStatus()).isEqualTo(OwnershipStatus.OWNED);
        assertThat(wardrobeClothes.getClothes().getId()).isEqualTo(99L);
        assertThat(wardrobeClothes.getClothes().getClothesInfoSource()).isEqualTo(ClothesInfoSource.PURCHASE_HISTORY);
        assertThat(response.clothesId()).isEqualTo(99L);
        verify(clothesTagHelper).copyTagsFrom(sharedExternal, wardrobeClothes.getClothes());
    }

    @Test
    @DisplayName("외부 추천 상품이 아닌 위시리스트 보유 전환은 기존 CLOTHES 행을 그대로 갱신한다")
    void convertToOwned_mutatesLinkedClothesWhenNotExternalShopping() {
        Clothes personalClothes = createClothes(10L, ClothesInfoSource.PURCHASE_HISTORY);
        WardrobeClothes wardrobeClothes = createWishlistLink(personalClothes);

        given(wardrobeClothesRepository.findByClothesIdAndUserId(10L, 1L))
                .willReturn(Optional.of(wardrobeClothes));

        clothesService.convertToOwned(
                1L,
                10L,
                new ClothesConvertToOwnedRequest("PRODUCT-002", "M", "https://example.com/user.jpg", false)
        );

        assertThat(personalClothes.getClothesInfoSource()).isEqualTo(ClothesInfoSource.PURCHASE_HISTORY);
        assertThat(personalClothes.getProductCode()).isEqualTo("PRODUCT-002");
        assertThat(personalClothes.getExternalSource()).isEqualTo(Clothes.EXTERNAL_NONE);
        verify(clothesRepository, never()).save(any());
        verify(clothesTagHelper, never()).copyTagsFrom(any(), any());
    }

    private Clothes createClothes(Long clothesId, ClothesInfoSource infoSource) {
        Clothes clothes = Clothes.builder()
                .name("item-" + clothesId)
                .brandName("brand")
                .productCode("CODE-" + clothesId)
                .imageUrl("https://example.com/" + clothesId + ".jpg")
                .category("TOP")
                .itemType("SHORT_SLEEVE")
                .season(ClothesSeason.SUMMER)
                .clothesInfoSource(infoSource)
                .externalSource(infoSource == ClothesInfoSource.EXTERNAL_SHOPPING ? "MUSINSA" : Clothes.EXTERNAL_NONE)
                .externalProductId(infoSource == ClothesInfoSource.EXTERNAL_SHOPPING ? "ext-1" : Clothes.EXTERNAL_NONE)
                .externalProductUrl(infoSource == ClothesInfoSource.EXTERNAL_SHOPPING ? "https://shop.example.com/1" : Clothes.EXTERNAL_NONE)
                .isVerified(false)
                .build();
        ReflectionTestUtils.setField(clothes, "id", clothesId);
        clothes.addColorTag(ClothingColor.create(clothes, "WHITE", ColorRole.PRIMARY, (byte) 1));
        Style style = Style.from(StyleCode.CASUAL);
        ReflectionTestUtils.setField(style, "id", 1L);
        clothes.addStyleTag(ClothesStyleTag.create(clothes, style, StyleRole.PRIMARY, (byte) 1));
        return clothes;
    }

    private WardrobeClothes createWishlistLink(Clothes clothes) {
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
                .ownershipStatus(OwnershipStatus.WISHLIST)
                .size("FREE")
                .favorite(false)
                .userImageUrl("https://example.com/user.jpg")
                .build();
        ReflectionTestUtils.setField(wardrobeClothes, "id", 20L);
        return wardrobeClothes;
    }
}
