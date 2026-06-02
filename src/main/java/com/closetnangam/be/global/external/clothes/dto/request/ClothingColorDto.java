package com.closetnangam.be.global.external.clothes.dto.request;

import com.closetnangam.be.domain.clothes.enums.ColorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ClothingColorDto(
        @NotBlank @Size(max = 50) String colorCode,
        @NotNull ColorRole colorRole,
        @NotNull @PositiveOrZero Byte sortOrder
) {
}
