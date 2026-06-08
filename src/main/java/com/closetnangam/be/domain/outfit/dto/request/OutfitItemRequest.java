package com.closetnangam.be.domain.outfit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OutfitItemRequest {

    @NotNull
    private Long clothesId;

    @NotNull
    private String itemRole;

    @NotNull
    private Integer layerOrder;
}
