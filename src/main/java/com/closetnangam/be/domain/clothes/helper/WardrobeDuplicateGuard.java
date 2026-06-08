package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WardrobeDuplicateGuard {

    private final WardrobeClothesRepository wardrobeClothesRepository;

    public void rejectIfAlreadyInWardrobe(
            Long userId,
            String brandName,
            String name,
            String category,
            String itemType,
            String primaryColor,
            String productCode
    ) {
        String normalizedBrand = normalizeLabel(brandName);
        String normalizedName = normalizeLabel(name);
        String normalizedColor = normalizeLabel(primaryColor);

        if (StringUtils.hasText(productCode) && !isEphemeralProductCode(productCode)) {
            if (wardrobeClothesRepository.existsActiveByUserIdAndProductCode(userId, productCode.trim())) {
                throw new IllegalStateException("이미 보유 중인 품번입니다.");
            }
        }

        if (wardrobeClothesRepository.existsActiveByUserIdAndIdentity(
                userId,
                normalizedBrand,
                normalizedName,
                category,
                itemType,
                normalizedColor,
                OwnershipStatus.OWNED
        )) {
            throw new IllegalStateException("이미 보유 중인 옷입니다.");
        }

        if (wardrobeClothesRepository.existsActiveByUserIdAndIdentity(
                userId,
                normalizedBrand,
                normalizedName,
                category,
                itemType,
                normalizedColor,
                OwnershipStatus.WISHLIST
        )) {
            throw new IllegalStateException("이미 위시리스트에 등록된 옷입니다.");
        }
    }

    private String normalizeLabel(String value) {
        return StringUtils.hasText(value) ? value.trim() : "UNKNOWN";
    }

    private boolean isEphemeralProductCode(String productCode) {
        String trimmed = productCode.trim();
        return trimmed.startsWith("PHOTO-")
                || trimmed.startsWith("PURCHASE-")
                || "UNKNOWN".equalsIgnoreCase(trimmed);
    }
}
