package com.closetnangam.be.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClothesConvertToOwnedRequest(
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 50) String size,
        @Size(max = 50) String season,
        @NotBlank @Size(max = 500) String userImageUrl,
        @NotNull Boolean isVerified
) {
}
