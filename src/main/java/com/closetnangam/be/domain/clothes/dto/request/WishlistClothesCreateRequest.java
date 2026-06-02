package com.closetnangam.be.domain.clothes.dto.request;

import com.closetnangam.be.domain.catalog.constants.CatalogLimits;
import com.closetnangam.be.global.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WishlistClothesCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String brandName,
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 500) @Pattern(regexp = ValidationPatterns.HTTP_URL) String imageUrl,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 50) String itemType,
        @NotBlank @Size(max = 50) String primaryColor,
        @Size(max = CatalogLimits.MAX_SECONDARY_COLORS) List<@NotBlank String> secondaryColors,
        @NotEmpty @Size(max = CatalogLimits.MAX_STYLES) List<@NotBlank String> styles,
        @NotBlank @Size(max = 50) String size,
        @Size(max = 50) String season,
        @NotBlank @Size(max = 50) String externalSource,
        @NotBlank @Size(max = 255) String externalProductId,
        @NotBlank @Size(max = 500) @Pattern(regexp = ValidationPatterns.HTTP_URL) String externalProductUrl
) {
}
