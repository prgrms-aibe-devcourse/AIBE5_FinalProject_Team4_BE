package com.closetnangam.be.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WishlistClothesCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String brandName,
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 500) String imageUrl,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 50) String itemType,
        @NotBlank @Size(max = 50) String color,
        @NotEmpty List<@NotBlank String> styles,
        @NotBlank @Size(max = 50) String externalSource,
        @NotBlank @Size(max = 255) String externalProductId,
        @NotBlank @Size(max = 500) String externalProductUrl
) {
}
