package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;

import java.time.LocalDateTime;
import java.util.List;

public record ClothesResponse(
        Long clothesId,
        Long wardrobeClothesId,
        Long wardrobeId,
        Long userId,
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
        OwnershipStatus ownershipStatus,
        ClothesInfoSource clothesInfoSource,
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
                resolveWardrobeId(wardrobeClothes),
                resolveUserId(wardrobeClothes),
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
                wardrobeClothes != null ? wardrobeClothes.getOwnershipStatus() : null,
                clothes.getClothesInfoSource(),
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

    private static Long resolveWardrobeId(WardrobeClothes wardrobeClothes) {
        if (wardrobeClothes == null) {
            return null;
        }
        Wardrobe wardrobe = wardrobeClothes.getWardrobe();
        return wardrobe != null ? wardrobe.getId() : null;
    }

    private static Long resolveUserId(WardrobeClothes wardrobeClothes) {
        if (wardrobeClothes == null) {
            return null;
        }
        Wardrobe wardrobe = wardrobeClothes.getWardrobe();
        if (wardrobe == null || wardrobe.getUser() == null) {
            return null;
        }
        return wardrobe.getUser().getId();
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
