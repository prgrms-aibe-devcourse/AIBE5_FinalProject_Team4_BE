package com.closetnangam.be.domain.outfit.dto.response;

import com.closetnangam.be.domain.outfit.entity.Outfit;

import java.time.LocalDateTime;

public record OutfitResponse(
        Long outfitId,
        Long outfitBookId,
        String title,
        String description,
        String thumbnailUrl,
        String situation,
        String season,
        boolean favorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutfitResponse from(Outfit outfit) {
        return new OutfitResponse(
                outfit.getOutfitId(),
                outfit.getOutfitBook().getId(),
                outfit.getTitle(),
                outfit.getDescription(),
                outfit.getThumbnailUrl(),
                outfit.getSituation(),
                outfit.getSeason(),
                outfit.isFavorite(),
                outfit.getCreatedAt(),
                outfit.getUpdatedAt()
        );
    }
}
