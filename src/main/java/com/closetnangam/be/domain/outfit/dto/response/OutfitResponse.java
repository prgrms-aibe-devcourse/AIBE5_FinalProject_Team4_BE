package com.closetnangam.be.domain.outfit.dto.response;

import com.closetnangam.be.domain.outfit.entity.Outfit;

import java.time.LocalDateTime;
import java.util.List;

public record OutfitResponse(
        Long outfitId,
        Long outfitBookId,
        String title,
        String description,
        String thumbnailUrl,
        String situation,
        String season,
        boolean favorite,
        List<OutfitItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutfitResponse from(Outfit outfit) {
        return from(outfit, List.of());
    }

    public static OutfitResponse from(Outfit outfit, List<OutfitItemResponse> items) {
        /*
         * items는 코디북 재조회에서 저장된 OUTFIT_ITEMS를 복원하기 위한 확장 필드다.
         * 기존 코디 생성처럼 구성 아이템을 아직 만들지 않는 경로는 빈 목록으로 내려 호환성을 유지한다.
         */
        return new OutfitResponse(
                outfit.getOutfitId(),
                outfit.getOutfitBook().getId(),
                outfit.getTitle(),
                outfit.getDescription(),
                outfit.getThumbnailUrl(),
                outfit.getSituation(),
                outfit.getSeason(),
                outfit.isFavorite(),
                items == null ? List.of() : items,
                outfit.getCreatedAt(),
                outfit.getUpdatedAt()
        );
    }
}
