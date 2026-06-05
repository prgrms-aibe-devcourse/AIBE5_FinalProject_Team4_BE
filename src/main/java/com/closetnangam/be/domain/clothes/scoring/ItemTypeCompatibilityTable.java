package com.closetnangam.be.domain.clothes.scoring;

import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.catalog.enums.StyleCode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * itemType(소분류) 간 코디 어울림 점수표.
 *
 * <p>1) {@link CohesionGroup} 기반 Jaccard — enum 전체 itemType 커버<br>
 * 2) 대표 조합 명시 페어 — GOOD(1.0) / SOSO(0.5) / CLASH(0.3)
 */
public final class ItemTypeCompatibilityTable {

    public static final double PERFECT = 1.0;
    public static final double GOOD  = 0.9;
    public static final double SOSO  = 0.7;
    public static final double NEUTRAL = 0.5;
    public static final double CLASH = 0.3;

    private enum CohesionGroup {
        CASUAL, SMART, FORMAL, ATHLETIC, STREET, OUTDOOR, SUMMER, WINTER
    }

    private static final Map<String, Set<CohesionGroup>> GROUPS;
    private static final Map<String, Double> EXPLICIT_PAIRS;

    static {
        GROUPS = new HashMap<>();
        for (ClothesItemType itemType : ClothesItemType.values()) {
            GROUPS.put(itemType.name(), EnumSet.noneOf(CohesionGroup.class));
        }

        // TOP
        groups("LONG_SLEEVE", CohesionGroup.CASUAL, CohesionGroup.SMART);
        groups("SHORT_SLEEVE", CohesionGroup.CASUAL, CohesionGroup.SUMMER);
        groups("SHIRT", CohesionGroup.SMART, CohesionGroup.FORMAL);
        groups("HOODIE", CohesionGroup.CASUAL, CohesionGroup.STREET, CohesionGroup.ATHLETIC);
        groups("SWEAT", CohesionGroup.CASUAL, CohesionGroup.ATHLETIC);
        groups("COLLAR_TEE", CohesionGroup.CASUAL, CohesionGroup.SMART);
        groups("SLEEVELESS", CohesionGroup.CASUAL, CohesionGroup.SUMMER, CohesionGroup.ATHLETIC);
        groups("KNIT", CohesionGroup.SMART, CohesionGroup.WINTER, CohesionGroup.FORMAL);

        // BOTTOM
        groups("DENIM", CohesionGroup.CASUAL, CohesionGroup.STREET);
        groups("TRAINING", CohesionGroup.ATHLETIC, CohesionGroup.CASUAL);
        groups("COTTON", CohesionGroup.CASUAL, CohesionGroup.SMART);
        groups("SLACKS", CohesionGroup.SMART, CohesionGroup.FORMAL);
        groups("SHORTS", CohesionGroup.CASUAL, CohesionGroup.SUMMER, CohesionGroup.ATHLETIC);
        groups("CARGO", CohesionGroup.STREET, CohesionGroup.OUTDOOR, CohesionGroup.ATHLETIC);
        groups("SKIRT", CohesionGroup.SMART, CohesionGroup.FORMAL, CohesionGroup.SUMMER);

        // OUTER
        groups("WINDBREAKER", CohesionGroup.OUTDOOR, CohesionGroup.ATHLETIC, CohesionGroup.CASUAL);
        groups("HOOD_ZIPUP", CohesionGroup.CASUAL, CohesionGroup.STREET, CohesionGroup.ATHLETIC);
        groups("TRAINING_JACKET", CohesionGroup.ATHLETIC);
        groups("BLOUSON", CohesionGroup.SMART, CohesionGroup.CASUAL);
        groups("MA1", CohesionGroup.STREET, CohesionGroup.CASUAL);
        groups("VARSITY_JACKET", CohesionGroup.STREET, CohesionGroup.CASUAL, CohesionGroup.ATHLETIC);
        groups("LEATHER_JACKET", CohesionGroup.STREET, CohesionGroup.SMART);
        groups("SHEARLING", CohesionGroup.WINTER, CohesionGroup.STREET);
        groups("FLEECE_JACKET", CohesionGroup.CASUAL, CohesionGroup.WINTER, CohesionGroup.ATHLETIC);
        groups("VEST", CohesionGroup.SMART, CohesionGroup.CASUAL);
        groups("WORK_JACKET", CohesionGroup.OUTDOOR, CohesionGroup.STREET);
        groups("DENIM_JACKET", CohesionGroup.CASUAL, CohesionGroup.STREET);
        groups("BLAZER", CohesionGroup.FORMAL, CohesionGroup.SMART);
        groups("COACH_JACKET", CohesionGroup.CASUAL, CohesionGroup.STREET);
        groups("PADDING", CohesionGroup.WINTER);
        groups("LIGHT_PADDING", CohesionGroup.WINTER, CohesionGroup.CASUAL);
        groups("SINGLE_COAT", CohesionGroup.FORMAL, CohesionGroup.WINTER, CohesionGroup.SMART);
        groups("DOUBLE_COAT", CohesionGroup.FORMAL, CohesionGroup.WINTER);
        groups("BALMACAAN_COAT", CohesionGroup.FORMAL, CohesionGroup.SMART, CohesionGroup.WINTER);
        groups("TTEOKBOKKI_COAT", CohesionGroup.WINTER, CohesionGroup.CASUAL, CohesionGroup.STREET);

        // SHOES
        groups("SNEAKERS", CohesionGroup.CASUAL, CohesionGroup.ATHLETIC, CohesionGroup.STREET);
        groups("SPORTS_SHOES", CohesionGroup.ATHLETIC, CohesionGroup.OUTDOOR);
        groups("LOAFER", CohesionGroup.SMART, CohesionGroup.FORMAL);
        groups("DERBY", CohesionGroup.FORMAL, CohesionGroup.SMART);
        groups("BOOTS", CohesionGroup.WINTER, CohesionGroup.OUTDOOR, CohesionGroup.STREET);
        groups("SANDALS_SLIPPERS", CohesionGroup.SUMMER, CohesionGroup.CASUAL);
        groups("FLAT", CohesionGroup.SMART, CohesionGroup.SUMMER, CohesionGroup.FORMAL);
        groups("HEEL", CohesionGroup.FORMAL, CohesionGroup.SMART);

        EXPLICIT_PAIRS = new HashMap<>();

        // TOP ↔ BOTTOM
        pair("SHIRT", "SLACKS", GOOD);
        pair("SHIRT", "DENIM", GOOD);
        pair("SHIRT", "COTTON", GOOD);
        pair("SHIRT", "SKIRT", GOOD);
        pair("SHIRT", "SHORTS", SOSO);
        pair("HOODIE", "DENIM", GOOD);
        pair("HOODIE", "TRAINING", GOOD);
        pair("HOODIE", "CARGO", GOOD);
        pair("HOODIE", "SLACKS", SOSO);
        pair("HOODIE", "SKIRT", CLASH);
        pair("KNIT", "SLACKS", GOOD);
        pair("KNIT", "SKIRT", GOOD);
        pair("KNIT", "DENIM", SOSO);
        pair("SLEEVELESS", "SHORTS", GOOD);
        pair("SLEEVELESS", "SKIRT", GOOD);
        pair("SWEAT", "TRAINING", GOOD);
        pair("SWEAT", "SHORTS", GOOD);

        // BOTTOM ↔ SHOES
        pair("SHORTS", "SNEAKERS", GOOD);
        pair("SHORTS", "SANDALS_SLIPPERS", GOOD);
        pair("SHORTS", "SPORTS_SHOES", GOOD);
        pair("SHORTS", "BOOTS", SOSO);
        pair("SHORTS", "HEEL", CLASH);
        pair("SKIRT", "HEEL", GOOD);
        pair("SKIRT", "FLAT", GOOD);
        pair("SKIRT", "LOAFER", GOOD);
        pair("SKIRT", "SNEAKERS", SOSO);
        pair("SKIRT", "SPORTS_SHOES", CLASH);
        pair("SLACKS", "LOAFER", GOOD);
        pair("SLACKS", "DERBY", GOOD);
        pair("SLACKS", "SNEAKERS", SOSO);
        pair("DENIM", "SNEAKERS", GOOD);
        pair("DENIM", "BOOTS", GOOD);
        pair("DENIM", "LOAFER", SOSO);
        pair("TRAINING", "SPORTS_SHOES", GOOD);
        pair("TRAINING", "SNEAKERS", GOOD);
        pair("CARGO", "BOOTS", GOOD);
        pair("CARGO", "SNEAKERS", GOOD);

        // TOP ↔ OUTER
        pair("SHIRT", "BLAZER", GOOD);
        pair("SHIRT", "SINGLE_COAT", GOOD);
        pair("SHIRT", "VEST", GOOD);
        pair("KNIT", "SINGLE_COAT", GOOD);
        pair("KNIT", "BALMACAAN_COAT", GOOD);
        pair("HOODIE", "BLAZER", CLASH);
        pair("HOODIE", "SINGLE_COAT", CLASH);
        pair("HOODIE", "HOOD_ZIPUP", GOOD);
        pair("HOODIE", "DENIM_JACKET", GOOD);
        pair("HOODIE", "PADDING", SOSO);
        pair("SWEAT", "HOOD_ZIPUP", GOOD);
        pair("SWEAT", "FLEECE_JACKET", GOOD);
        pair("SHIRT", "PADDING", CLASH);

        // BOTTOM ↔ OUTER
        pair("SLACKS", "BLAZER", GOOD);
        pair("SLACKS", "SINGLE_COAT", GOOD);
        pair("SHORTS", "BLAZER", CLASH);
        pair("SHORTS", "PADDING", SOSO);
        pair("SKIRT", "BLAZER", GOOD);
        pair("DENIM", "DENIM_JACKET", GOOD);
        pair("DENIM", "LEATHER_JACKET", GOOD);
        pair("TRAINING", "TRAINING_JACKET", GOOD);
        pair("TRAINING", "WINDBREAKER", GOOD);

        // TOP/BOTTOM ↔ SHOES (카테고리가 다를 때)
        pair("SHIRT", "LOAFER", GOOD);
        pair("SHIRT", "DERBY", GOOD);
        pair("SHIRT", "SNEAKERS", SOSO);
        pair("HOODIE", "SNEAKERS", GOOD);
        pair("BLAZER", "LOAFER", GOOD);
        pair("PADDING", "BOOTS", GOOD);
        pair("PADDING", "SANDALS_SLIPPERS", CLASH);
        pair("SINGLE_COAT", "BOOTS", GOOD);
        pair("SINGLE_COAT", "SANDALS_SLIPPERS", CLASH);
        pair("SINGLE_COAT", "LOAFER", GOOD);

        // STYLE COMPATIBILITY
        for (StyleCode style : StyleCode.values()) {
            pair(style.name(), style.name(), PERFECT);
        }

        // CASUAL 관련
        pair("CASUAL", "CITYBOY", GOOD);
        pair("CASUAL", "RETRO", GOOD);
        pair("CASUAL", "STREET", SOSO);
        pair("CASUAL", "MINIMAL", SOSO);
        pair("CASUAL", "SPORTY", SOSO);
        pair("CASUAL", "WORKWEAR", SOSO);

        // STREET 관련
        pair("STREET", "WORKWEAR", GOOD);
        pair("STREET", "GORPCORE", GOOD);
        pair("STREET", "RETRO", SOSO);
        pair("STREET", "SPORTY", SOSO);

        // MINIMAL 관련
        pair("MINIMAL", "CHIC", GOOD);
        pair("MINIMAL", "CLASSIC", GOOD);
        pair("MINIMAL", "CITYBOY", SOSO);

        // CLASSIC 관련
        pair("CLASSIC", "CHIC", GOOD);
        pair("CLASSIC", "RETRO", SOSO);

        // GORPCORE 관련
        pair("GORPCORE", "WORKWEAR", GOOD);
        pair("GORPCORE", "SPORTY", GOOD);

        // CITYBOY 관련
        pair("CITYBOY", "WORKWEAR", GOOD);
    }

    private ItemTypeCompatibilityTable() {}

    /**
     * anchor와 candidate의 코디 어울림 점수 (0.0 ~ 1.0).
     * itemType 또는 styleCode 모두 지원합니다.
     * 명시 페어가 있으면 우선 적용, 없으면 itemType의 경우 cohesion group Jaccard 점수를 사용합니다.
     */
    public static double score(String anchor, String candidate) {
        if (anchor == null || candidate == null) {
            return NEUTRAL;
        }
        if (anchor.equals(candidate)) {
            Double explicit = lookupExplicit(anchor, candidate);
            if (explicit != null) return explicit;
            return SOSO;
        }

        Double explicit = lookupExplicit(anchor, candidate);
        if (explicit != null) {
            return explicit;
        }
        return groupScore(anchor, candidate);
    }

    private static Double lookupExplicit(String a, String b) {
        Double forward = EXPLICIT_PAIRS.get(pairKey(a, b));
        if (forward != null) {
            return forward;
        }
        return EXPLICIT_PAIRS.get(pairKey(b, a));
    }

    private static double groupScore(String anchorItemType, String candidateItemType) {
        Set<CohesionGroup> anchorGroups = GROUPS.getOrDefault(anchorItemType, Collections.emptySet());
        Set<CohesionGroup> candidateGroups = GROUPS.getOrDefault(candidateItemType, Collections.emptySet());

        if (anchorGroups.isEmpty() || candidateGroups.isEmpty()) {
            return NEUTRAL;
        }

        long intersection = anchorGroups.stream().filter(candidateGroups::contains).count();
        long union = anchorGroups.size() + candidateGroups.size() - intersection;
        if (union == 0) {
            return NEUTRAL;
        }

        double jaccard = (double) intersection / union;
        return 0.2 + 0.8 * jaccard;
    }

    private static void groups(String itemType, CohesionGroup... cohesionGroups) {
        GROUPS.get(itemType).addAll(Set.of(cohesionGroups));
    }

    private static void pair(String a, String b, double score) {
        EXPLICIT_PAIRS.put(pairKey(a, b), score);
    }

    private static String pairKey(String a, String b) {
        return a + "|" + b;
    }
}
