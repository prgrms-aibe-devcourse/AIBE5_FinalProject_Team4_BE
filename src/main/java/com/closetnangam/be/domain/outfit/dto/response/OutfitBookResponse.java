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
        return from(outfitBook, userId, outfits, List.of());
    }

    public static OutfitBookResponse from(
            OutfitBook outfitBook,
            Long userId,
            List<Outfit> outfits,
            List<OutfitResponse> outfitResponses
    ) {
        /*
         * outfitResponses가 제공되는 조회 경로는 저장된 OutfitItem까지 포함해 코디 구성을 복원한다.
         * 코디북 생성 직후처럼 구성 아이템이 없는 경로는 기존처럼 Outfit 메타데이터만 변환한다.
         */
        List<OutfitResponse> responses = outfitResponses == null || outfitResponses.isEmpty()
                ? outfits.stream()
                        .map(OutfitResponse::from)
                        .toList()
                : outfitResponses;
        return new OutfitBookResponse(
                outfitBook.getId(),
                userId,
                outfits.size(),
                responses,
                null,
                null
        );
    }
}
