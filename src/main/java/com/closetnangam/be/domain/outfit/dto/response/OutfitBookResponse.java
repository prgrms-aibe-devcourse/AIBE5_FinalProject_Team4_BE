package com.closetnangam.be.domain.outfit.dto.response;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;

import java.time.LocalDateTime;
import java.util.List;

public record OutfitBookResponse(
        Long outfitBookId,
        Long userId,
        int outfitCount,
        List<OutfitResponse> outfits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutfitBookResponse from(OutfitBook outfitBook, Long userId, List<Outfit> outfits) {
        return new OutfitBookResponse(
                outfitBook.getId(),
                userId,
                outfits.size(),
                outfits.stream()
                        .map(OutfitResponse::from)
                        .toList(),
                outfitBook.getCreatedAt(),
                outfitBook.getUpdatedAt()
        );
    }
}
