package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record ClothesResponse(
        Long clothesId,
        Long wardrobeId,
        Long userId,
        String name,
        String brandName,
        String productCode,
        String imageUrl,
        String category,
        String itemType,
        String color,
        ColorDisplayResponse colorDisplay,
        List<StyleTagResponse> styles,
        SourceType sourceType,
        String externalSource,
        String externalProductId,
        String externalProductUrl,
        Boolean isVerified,
        Boolean isFavorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ClothesResponse from(Clothes clothes) {
        ClothesColor clothesColor = ClothesColor.fromCode(clothes.getColor());

        List<StyleTagResponse> styles = clothes.getStyleTags().stream()
                .map(tag -> new StyleTagResponse(
                        tag.getStyle().getId(),
                        tag.getStyle().getCode(),
                        tag.getStyle().getName()
                ))
                .toList();

        return new ClothesResponse(
                clothes.getId(),
                clothes.getWardrobe().getId(),
                clothes.getWardrobe().getUser().getId(),
                clothes.getName(),
                clothes.getBrandName(),
                clothes.getProductCode(),
                clothes.getImageUrl(),
                clothes.getCategory(),
                clothes.getItemType(),
                clothes.getColor(),
                new ColorDisplayResponse(
                        clothesColor.name(),
                        clothesColor.getLabel(),
                        clothesColor.getHex()
                ),
                styles,
                clothes.getSourceType(),
                clothes.getExternalSource(),
                clothes.getExternalProductId(),
                clothes.getExternalProductUrl(),
                clothes.getIsVerified(),
                clothes.getIsFavorite(),
                clothes.getCreatedAt(),
                clothes.getUpdatedAt()
        );
    }

    public record ColorDisplayResponse(
            String code,
            String name,
            String hex
    ) {
    }

    public record StyleTagResponse(
            Long styleId,
            String code,
            String name
    ) {
    }
}
