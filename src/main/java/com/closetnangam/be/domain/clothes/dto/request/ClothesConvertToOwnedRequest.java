package com.closetnangam.be.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClothesConvertToOwnedRequest(
        @NotBlank @Size(max = 100) String productCode,
        @NotNull Boolean isVerified
) {
}
