package com.closetnangam.be.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ClothesUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String brandName,
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 500) String imageUrl,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 50) String itemType,
        @NotBlank @Size(max = 50) String primaryColor,
        @Size(max = 10) List<@NotBlank String> secondaryColors,
        @NotEmpty @Size(max = 10) List<@NotBlank String> styles,
        @NotBlank @Size(max = 50) String size,
        @Size(max = 50) String season,
        @NotNull Boolean isVerified
) {
}
