package com.closetnangam.be.domain.catalog.service;

import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CategoryCatalogServiceColorTest {

    private final CategoryCatalogService categoryCatalogService =
            new CategoryCatalogService(mock(StyleRepository.class));

    @Test
    @DisplayName("resolveClothesColors는 alias를 카탈로그 enum code로 정규화한다")
    void resolveClothesColors_normalizesColorAliases() {
        CategoryCatalogService.ResolvedClothesColors resolved =
                categoryCatalogService.resolveClothesColors("BLUE", List.of("INDIGO"));

        assertThat(resolved.primaryColor()).isEqualTo("LIGHT_BLUE");
        assertThat(resolved.secondaryColors()).containsExactly("NAVY");
    }
}
