package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ClothesItemType {

    LONG_SLEEVE(ClothesCategory.TOP, "롱슬리브", "긴소매 티셔츠"),
    SHORT_SLEEVE(ClothesCategory.TOP, "반팔", "반소매 티셔츠"),
    SHIRT(ClothesCategory.TOP, "셔츠", "셔츠"),
    HOODIE(ClothesCategory.TOP, "후드", "후드"),
    SWEAT(ClothesCategory.TOP, "스웨트", "스웨트"),
    COLLAR_TEE(ClothesCategory.TOP, "카라 티셔츠", "카라 티셔츠"),
    SLEEVELESS(ClothesCategory.TOP, "민소매", "민소매"),
    KNIT(ClothesCategory.TOP, "니트", "니트"),

    DENIM(ClothesCategory.BOTTOM, "데님", "데님"),
    TRAINING(ClothesCategory.BOTTOM, "트레이닝", "트레이닝"),
    COTTON(ClothesCategory.BOTTOM, "코튼", "면"),
    SLACKS(ClothesCategory.BOTTOM, "슬랙스", "슬랙스"),
    SHORTS(ClothesCategory.BOTTOM, "숏츠", "숏츠"),
    CARGO(ClothesCategory.BOTTOM, "카고", "카고"),
    SKIRT(ClothesCategory.BOTTOM, "스커트", "스커트"),

    WINDBREAKER(ClothesCategory.OUTER, "윈드브레이커", "바람막이"),
    HOOD_ZIPUP(ClothesCategory.OUTER, "후드집업", "후드집업"),
    TRAINING_JACKET(ClothesCategory.OUTER, "트레이닝 자켓", "트레이닝 자켓"),
    BLOUSON(ClothesCategory.OUTER, "블루종", "블루종"),
    MA1(ClothesCategory.OUTER, "MA-1", "MA-1"),
    VARSITY_JACKET(ClothesCategory.OUTER, "바시티 자켓", "바시티 자켓"),
    LEATHER_JACKET(ClothesCategory.OUTER, "레더 자켓", "가죽"),
    SHEARLING(ClothesCategory.OUTER, "무스탕", "무스탕"),
    FLEECE_JACKET(ClothesCategory.OUTER, "플리스 자켓", "후리스"),
    VEST(ClothesCategory.OUTER, "베스트", "조끼"),
    WORK_JACKET(ClothesCategory.OUTER, "워크자켓", "워크자켓"),
    DENIM_JACKET(ClothesCategory.OUTER, "데님자켓", "데님자켓"),
    BLAZER(ClothesCategory.OUTER, "블레이저", "블레이저"),
    COACH_JACKET(ClothesCategory.OUTER, "코치자켓", "코치자켓"),
    PADDING(ClothesCategory.OUTER, "패딩", "패딩"),
    LIGHT_PADDING(ClothesCategory.OUTER, "경량패딩", "경량패딩"),
    SINGLE_COAT(ClothesCategory.OUTER, "싱글코트", "싱글코트"),
    DOUBLE_COAT(ClothesCategory.OUTER, "더블코트", "더블코트"),
    BALMACAAN_COAT(ClothesCategory.OUTER, "발마칸 코트", "발마칸 코트"),
    TTEOKBOKKI_COAT(ClothesCategory.OUTER, "떡볶이 코트", "떡볶이 코트"),

    SNEAKERS(ClothesCategory.SHOES, "스니커즈", "스니커즈"),
    SPORTS_SHOES(ClothesCategory.SHOES, "스포츠화", "운동화, 등산화 등"),
    LOAFER(ClothesCategory.SHOES, "로퍼", "로퍼"),
    DERBY(ClothesCategory.SHOES, "더비", "더비"),
    BOOTS(ClothesCategory.SHOES, "부츠", "부츠"),
    SANDALS_SLIPPERS(ClothesCategory.SHOES, "샌들/슬리퍼", "샌들/슬리퍼"),
    FLAT(ClothesCategory.SHOES, "플랫", "플랫"),
    HEEL(ClothesCategory.SHOES, "힐", "힐");

    private final ClothesCategory category;
    private final String label;
    private final String description;

    public static ClothesItemType fromCode(String code) {
        return ClothesItemType.valueOf(code);
    }

    public static ClothesItemType fromCodeOrDefault(String code) {
        if (code == null || code.isBlank()) return LONG_SLEEVE;
        try {
            return ClothesItemType.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return LONG_SLEEVE;
        }
    }

    public static List<ClothesItemType> byCategory(ClothesCategory category) {
        return Arrays.stream(values())
                .filter(itemType -> itemType.category == category)
                .toList();
    }

    public static boolean matchesCategory(String categoryCode, String itemTypeCode) {
        ClothesCategory category = ClothesCategory.fromCode(categoryCode);
        ClothesItemType itemType = ClothesItemType.fromCode(itemTypeCode);
        return itemType.category == category;
    }
}
