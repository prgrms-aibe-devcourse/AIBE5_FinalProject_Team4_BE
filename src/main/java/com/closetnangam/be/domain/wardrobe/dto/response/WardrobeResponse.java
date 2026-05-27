package com.closetnangam.be.domain.wardrobe.dto.response;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;

public record WardrobeResponse(
        Long wardrobeId,
        Long userId
) {

    public static WardrobeResponse from(Wardrobe wardrobe) {
        return new WardrobeResponse(
                wardrobe.getId(),
                wardrobe.getUser().getId()
        );
    }
}
