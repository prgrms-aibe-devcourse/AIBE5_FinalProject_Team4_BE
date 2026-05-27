package com.closetnangam.be.domain.clothes.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClothesFavoriteRequest(
        @NotNull Boolean isFavorite
) {
}
