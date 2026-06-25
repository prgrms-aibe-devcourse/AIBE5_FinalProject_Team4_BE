package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClothesTagHelperTest {

    @Mock
    private StyleRepository styleRepository;

    @Test
    @DisplayName("applyColorTags는 alias 입력을 정규화된 catalog code로 저장한다")
    void applyColorTags_storesNormalizedCatalogCodes() {
        CategoryCatalogService categoryCatalogService = new CategoryCatalogService(styleRepository);
        ClothesTagHelper clothesTagHelper = new ClothesTagHelper(styleRepository, categoryCatalogService);

        Clothes clothes = Clothes.builder()
                .name("데님 팬츠")
                .brandName("BRAND")
                .productCode("P-1")
                .imageUrl("https://example.com/item.jpg")
                .category("BOTTOM")
                .itemType("DENIM")
                .gender(ClothesGender.UNISEX)
                .season(ClothesSeason.ALL_SEASON)
                .clothesInfoSource(ClothesInfoSource.PHOTO)
                .externalSource(Clothes.EXTERNAL_NONE)
                .externalProductId(Clothes.EXTERNAL_NONE)
                .build();

        clothesTagHelper.applyColorTags(clothes, "BLUE", List.of("INDIGO"));

        assertThat(clothes.getSortedColorTags()).hasSize(2);
        assertThat(clothes.getSortedColorTags().get(0).getColorCode()).isEqualTo("LIGHT_BLUE");
        assertThat(clothes.getSortedColorTags().get(0).getColorRole()).isEqualTo(ColorRole.PRIMARY);
        assertThat(clothes.getSortedColorTags().get(1).getColorCode()).isEqualTo("NAVY");
        assertThat(clothes.getSortedColorTags().get(1).getColorRole()).isEqualTo(ColorRole.SECONDARY);
    }
}
