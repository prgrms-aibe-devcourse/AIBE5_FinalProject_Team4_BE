package com.closetnangam.be.global.external.clothes.dto.request;

import com.closetnangam.be.domain.clothes.enums.StyleRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ClothesStyleDto(
        @NotNull Long styleId,
        @NotNull StyleRole styleRole,
        @NotNull @PositiveOrZero Byte sortOrder
) {
}
