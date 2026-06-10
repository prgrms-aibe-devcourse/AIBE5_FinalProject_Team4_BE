package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WardrobeExclusionMatcherTest {

    @Test
    @DisplayName("보유 복제본과 동일 identity의 원본 외부 상품을 제외한다")
    void excludesOriginalExternalProductMatchingOwnedCloneIdentity() {
        Clothes ownedClone = createExternalClothes(99L, "slacks");
        ReflectionTestUtils.setField(ownedClone, "clothesInfoSource", ClothesInfoSource.PURCHASE_HISTORY);
        ReflectionTestUtils.setField(ownedClone, "externalProductId", Clothes.EXTERNAL_NONE);

        Clothes originalExternal = createExternalClothes(200L, "slacks");
        ReflectionTestUtils.setField(originalExternal, "externalProductId", "ext-200");

        WardrobeExclusionMatcher.WardrobeExclusionIndex index =
                WardrobeExclusionMatcher.WardrobeExclusionIndex.fromActiveWardrobeEntries(
                        java.util.List.of(createOwnedLink(ownedClone))
                );

        assertThat(index.excludes(originalExternal)).isTrue();
        assertThat(index.excludes(createExternalClothes(201L, "other-item"))).isFalse();
    }

    private Clothes createExternalClothes(Long clothesId, String name) {
        Clothes clothes = Clothes.builder()
                .name(name)
                .brandName("brand")
                .productCode("CODE-" + clothesId)
                .imageUrl("https://example.com/" + clothesId + ".jpg")
                .category("BOTTOM")
                .itemType("SLACKS")
                .season(ClothesSeason.ALL_SEASON)
                .clothesInfoSource(ClothesInfoSource.EXTERNAL_SHOPPING)
                .externalSource("MUSINSA")
                .externalProductId("ext-" + clothesId)
                .externalProductUrl("https://shop.example.com/" + clothesId)
                .isVerified(false)
                .build();
        ReflectionTestUtils.setField(clothes, "id", clothesId);
        clothes.addColorTag(com.closetnangam.be.domain.clothes.entity.ClothingColor.create(
                clothes, "BLACK", ColorRole.PRIMARY, (byte) 1
        ));
        return clothes;
    }

    private WardrobeClothes createOwnedLink(Clothes clothes) {
        User user = User.builder()
                .nickname("user-1")
                .email("user1@example.com")
                .gender(User.Gender.MALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        Wardrobe wardrobe = Wardrobe.create(user);
        return WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(clothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size("L")
                .favorite(false)
                .userImageUrl("https://example.com/user.jpg")
                .build();
    }
}
