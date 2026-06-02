package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record ClothesResponse(
        Long clothesId,
        Long wardrobeClothesId,
        String name,
        String brandName,
        String productCode,
        String imageUrl,
        String category,
        String itemType,
        String primaryColor,
        ColorDisplayResponse primaryColorDisplay,
        List<SecondaryColorResponse> secondaryColors,
        List<StyleTagResponse> styles,
        SourceType sourceType,
        ClothesInfoSource infoSource,
        String externalSource,
        String externalProductId,
        String externalProductUrl,
        Boolean isVerified,
        Boolean isFavorite,
        String size,
        String season,
        String userImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ClothesResponse from(Clothes clothes) {
        return from(clothes, null);
    }

    public static ClothesResponse from(Clothes clothes, WardrobeClothes wardrobeClothes) {
        ClothingColor primaryColorTag = clothes.getSortedColorTags().stream()
                .filter(color -> color.getColorRole() == ColorRole.PRIMARY)
                .findFirst()
                .orElse(null);

        String primaryColorCode = primaryColorTag != null ? primaryColorTag.getColorCode() : null;
        ColorDisplayResponse primaryColorDisplay = primaryColorCode != null
                ? toColorDisplay(primaryColorCode)
                : null;

        List<SecondaryColorResponse> secondaryColors = clothes.getSortedColorTags().stream()
                .filter(color -> color.getColorRole() == ColorRole.SECONDARY)
                .map(color -> new SecondaryColorResponse(
                        color.getColorCode(),
                        toColorDisplay(color.getColorCode()),
                        color.getSortOrder()
                ))
                .toList();

        List<StyleTagResponse> styles = clothes.getSortedStyleTags().stream()
                .map(tag -> new StyleTagResponse(
                        tag.getStyle().getId(),
                        tag.getStyle().getCode(),
                        tag.getStyle().getName(),
                        tag.getStyleRole().name(),
                        tag.getSortOrder()
                ))
                .toList();

        return new ClothesResponse(
                clothes.getId(),
                wardrobeClothes != null ? wardrobeClothes.getId() : null,
                clothes.getName(),
                clothes.getBrandName(),
                clothes.getProductCode(),
                clothes.getImageUrl(),
                clothes.getCategory(),
                clothes.getItemType(),
                primaryColorCode,
                primaryColorDisplay,
                secondaryColors,
                styles,
                clothes.getSourceType(),
                clothes.getInfoSource(),
                clothes.getExternalSource(),
                clothes.getExternalProductId(),
                clothes.getExternalProductUrl(),
                clothes.getIsVerified(),
                wardrobeClothes != null ? wardrobeClothes.getFavorite() : null,
                wardrobeClothes != null ? wardrobeClothes.getSize() : null,
                wardrobeClothes != null ? wardrobeClothes.getSeason() : null,
                wardrobeClothes != null ? wardrobeClothes.getUserImageUrl() : null,
                clothes.getCreatedAt(),
                clothes.getUpdatedAt()
        );
    }

    private static ColorDisplayResponse toColorDisplay(String colorCode) {
        ClothesColor clothesColor = ClothesColor.fromCode(colorCode);
        return new ColorDisplayResponse(
                clothesColor.name(),
                clothesColor.getLabel(),
                clothesColor.getHex()
        );
    }

    public record ColorDisplayResponse(
            String code,
            String name,
            String hex
    ) {
    }

    public record SecondaryColorResponse(
            String code,
            ColorDisplayResponse colorDisplay,
            Byte sortOrder
    ) {
    }

    public record StyleTagResponse(
            Long styleId,
            String code,
            String name,
            String styleRole,
            Byte sortOrder
    ) {
    }
}
