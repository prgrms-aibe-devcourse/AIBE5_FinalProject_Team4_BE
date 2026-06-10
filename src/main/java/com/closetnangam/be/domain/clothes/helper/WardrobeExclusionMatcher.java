package com.closetnangam.be.domain.clothes.helper;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 사용자 옷장에 이미 연결된 옷과 동일·동등한 {@link Clothes} 후보를 제외합니다.
 *
 * <p>공용 {@link com.closetnangam.be.domain.clothes.enums.ClothesInfoSource#EXTERNAL_SHOPPING} 마스터를
 * 보유 전환해 복제한 뒤에도, 원본 외부 상품 ID 또는 브랜드·상품명·카테고리·타입·대표색상 기준으로
 * 추천/위시리스트 중복을 막습니다.
 */
@Component
@RequiredArgsConstructor
public class WardrobeExclusionMatcher {

    private final WardrobeClothesRepository wardrobeClothesRepository;

    public WardrobeExclusionIndex buildActiveExclusionIndex(Long userId) {
        List<WardrobeClothes> activeEntries = wardrobeClothesRepository.findAllActiveByUserIdAndOwnershipStatuses(
                userId,
                List.of(OwnershipStatus.OWNED, OwnershipStatus.WISHLIST)
        );
        return WardrobeExclusionIndex.fromActiveWardrobeEntries(activeEntries);
    }

    public void rejectIfEquivalentAlreadyInWardrobe(Long userId, Clothes candidate) {
        String externalProductKey = externalProductKey(candidate);
        if (externalProductKey != null
                && wardrobeClothesRepository.existsActiveByUserIdAndExternalProduct(
                userId,
                candidate.getExternalSource(),
                candidate.getExternalProductId()
        )) {
            throw new IllegalStateException("이미 옷장에 등록된 상품입니다.");
        }

        String primaryColor = resolvePrimaryColor(candidate);
        if (wardrobeClothesRepository.existsActiveByUserIdAndIdentity(
                userId,
                normalizeLabel(candidate.getBrandName()),
                normalizeLabel(candidate.getName()),
                candidate.getCategory(),
                candidate.getItemType(),
                primaryColor,
                OwnershipStatus.OWNED
        ) || wardrobeClothesRepository.existsActiveByUserIdAndIdentity(
                userId,
                normalizeLabel(candidate.getBrandName()),
                normalizeLabel(candidate.getName()),
                candidate.getCategory(),
                candidate.getItemType(),
                primaryColor,
                OwnershipStatus.WISHLIST
        )) {
            throw new IllegalStateException("이미 옷장에 등록된 상품입니다.");
        }
    }

    public record WardrobeExclusionIndex(
            Set<Long> linkedClothesIds,
            Set<String> externalProductKeys,
            Set<String> identityKeys
    ) {
        public static WardrobeExclusionIndex fromActiveWardrobeEntries(List<WardrobeClothes> entries) {
            Set<Long> linkedClothesIds = new HashSet<>();
            Set<String> externalProductKeys = new HashSet<>();
            Set<String> identityKeys = new HashSet<>();

            for (WardrobeClothes entry : entries) {
                Clothes clothes = entry.getClothes();
                if (clothes == null || clothes.getId() == null) {
                    continue;
                }
                linkedClothesIds.add(clothes.getId());

                String externalKey = externalProductKey(clothes);
                if (externalKey != null) {
                    externalProductKeys.add(externalKey);
                }
                identityKeys.add(identityKey(clothes));
            }

            return new WardrobeExclusionIndex(linkedClothesIds, externalProductKeys, identityKeys);
        }

        public boolean excludes(Clothes candidate) {
            if (candidate == null) {
                return true;
            }
            if (candidate.getId() != null && linkedClothesIds.contains(candidate.getId())) {
                return true;
            }
            String externalKey = externalProductKey(candidate);
            if (externalKey != null && externalProductKeys.contains(externalKey)) {
                return true;
            }
            return identityKeys.contains(identityKey(candidate));
        }
    }

    public static String externalProductKey(Clothes clothes) {
        if (clothes == null
                || !StringUtils.hasText(clothes.getExternalProductId())
                || Clothes.EXTERNAL_NONE.equals(clothes.getExternalProductId())
                || !StringUtils.hasText(clothes.getExternalSource())
                || Clothes.EXTERNAL_NONE.equals(clothes.getExternalSource())) {
            return null;
        }
        return clothes.getExternalSource().trim().toUpperCase(Locale.ROOT)
                + "|"
                + clothes.getExternalProductId().trim();
    }

    public static String identityKey(Clothes clothes) {
        return normalizeLabel(clothes.getBrandName())
                + "|"
                + normalizeLabel(clothes.getName())
                + "|"
                + clothes.getCategory()
                + "|"
                + clothes.getItemType()
                + "|"
                + normalizeLabel(resolvePrimaryColor(clothes));
    }

    private static String resolvePrimaryColor(Clothes clothes) {
        return clothes.getSortedColorTags().stream()
                .filter(color -> color.getColorRole() == ColorRole.PRIMARY)
                .map(color -> color.getColorCode())
                .findFirst()
                .orElse("UNKNOWN");
    }

    private static String normalizeLabel(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "unknown";
    }
}
