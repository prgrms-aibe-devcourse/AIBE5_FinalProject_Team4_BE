package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.AnchorItem;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.ColorInfo;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.RecommendedItem;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.ScoreBreakdown;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.ColorCompatibilityTable;
import com.closetnangam.be.domain.clothes.scoring.ItemTypeCompatibilityTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesRecommendationService {

    /**
     * 응답에서 카테고리를 보여줄 순서.
     * anchor 카테고리는 제외되므로 실제 응답에는 최대 3개 카테고리가 나타납니다.
     */
    private static final List<String> CATEGORY_ORDER = List.of("TOP", "BOTTOM", "OUTER", "SHOES");

    private static final int MAX_LIMIT_PER_CATEGORY = 10;

    // 점수 가중치 (합계 = 1.0)
    private static final double WEIGHT_COLOR     = 0.35;
    private static final double WEIGHT_STYLE     = 0.30;
    private static final double WEIGHT_SEASON    = 0.15;
    private static final double WEIGHT_ITEM_TYPE = 0.20;

    /** primary 색상은 full weight, secondary는 보조 색상으로 약한 weight 적용 */
    private static final double PRIMARY_COLOR_WEIGHT   = 1.0;
    private static final double SECONDARY_COLOR_WEIGHT = 0.6;

    private final WardrobeClothesRepository wardrobeClothesRepository;

    /**
     * 기준 옷({clothesId})와 어울리는 보유 옷을 카테고리별로 추천합니다.
     *
     * @param limitPerCategory 카테고리당 최대 추천 수 (1~10)
     */
    public ClothesRecommendationResponse recommend(Long userId, Long clothesId, int limitPerCategory) {
        int limit = Math.min(Math.max(1, limitPerCategory), MAX_LIMIT_PER_CATEGORY);

        WardrobeClothes anchor = wardrobeClothesRepository
                .findByClothesIdAndUserId(clothesId, userId)
                .orElseThrow(() -> new IllegalArgumentException("옷을 찾을 수 없습니다."));

        String excludeCategory = anchor.getClothes().getCategory();

        List<WardrobeClothes> candidates = wardrobeClothesRepository.findCandidatesForRecommendation(
                userId, OwnershipStatus.OWNED, clothesId, excludeCategory
        );

        Map<String, List<RecommendedItem>> recommendations = scoredAndGrouped(anchor, candidates, limit);

        return new ClothesRecommendationResponse(toAnchorItem(anchor), recommendations);
    }

    // ── 점수 계산 및 카테고리별 그룹화 ────────────────────────────────────

    private Map<String, List<RecommendedItem>> scoredAndGrouped(
            WardrobeClothes anchor,
            List<WardrobeClothes> candidates,
            int limit
    ) {
        Map<String, List<RecommendedItem>> grouped = candidates.stream()
                .map(candidate -> toRecommendedItem(anchor, candidate))
                .collect(Collectors.groupingBy(RecommendedItem::category));

        Map<String, List<RecommendedItem>> ordered = new LinkedHashMap<>();
        for (String category : CATEGORY_ORDER) {
            if (grouped.containsKey(category)) {
                List<RecommendedItem> sorted = grouped.get(category).stream()
                        .sorted(Comparator.comparingInt(RecommendedItem::compatibilityScore).reversed())
                        .limit(limit)
                        .toList();
                ordered.put(category, sorted);
            }
        }
        return ordered;
    }

    private RecommendedItem toRecommendedItem(WardrobeClothes anchor, WardrobeClothes candidate) {
        Clothes anchorClothes = anchor.getClothes();
        Clothes candidateClothes = candidate.getClothes();

        double colorScore     = computeColorScore(anchorClothes, candidateClothes);
        double styleScore     = computeStyleScore(anchorClothes, candidateClothes);
        double seasonScore    = computeSeasonScore(anchor.getSeason(), candidate.getSeason());
        double itemTypeScore  = computeItemTypeScore(anchorClothes.getItemType(), candidateClothes.getItemType());

        int total = (int) Math.round(
                100.0 * (
                        WEIGHT_COLOR * colorScore
                                + WEIGHT_STYLE * styleScore
                                + WEIGHT_SEASON * seasonScore
                                + WEIGHT_ITEM_TYPE * itemTypeScore
                )
        );
        ScoreBreakdown breakdown = new ScoreBreakdown(
                (int) Math.round(colorScore * 100),
                (int) Math.round(styleScore * 100),
                (int) Math.round(seasonScore * 100),
                (int) Math.round(itemTypeScore * 100)
        );

        String primaryColor = getPrimaryColorCode(candidateClothes);

        return new RecommendedItem(
                candidateClothes.getId(),
                candidate.getId(),
                candidateClothes.getName(),
                candidateClothes.getImageUrl(),
                candidate.getUserImageUrl(),
                candidateClothes.getCategory(),
                candidateClothes.getItemType(),
                primaryColor,
                toColorInfo(primaryColor),
                getSecondaryColorCodes(candidateClothes),
                getStyleCodes(candidateClothes),
                candidate.getSeason(),
                total,
                breakdown
        );
    }

    // ── 개별 점수 계산 ────────────────────────────────────────────────────

    /**
     * 색상 어울림 점수 (0.0 ~ 1.0).
     * 양쪽 옷의 primary·secondary 색상 조합을 양방향으로 비교하고,
     * secondary 색상은 {@link #SECONDARY_COLOR_WEIGHT}를 곱해 primary보다 약하게 반영합니다.
     */
    private double computeColorScore(Clothes anchor, Clothes candidate) {
        List<WeightedColor> anchorColors = getWeightedColors(anchor);
        List<WeightedColor> candidateColors = getWeightedColors(candidate);

        if (anchorColors.isEmpty() || candidateColors.isEmpty()) {
            return 0.5;
        }

        double best = 0.0;
        for (WeightedColor anchorColor : anchorColors) {
            for (WeightedColor candidateColor : candidateColors) {
                double harmony = Math.max(
                        ColorCompatibilityTable.score(anchorColor.code(), candidateColor.code()),
                        ColorCompatibilityTable.score(candidateColor.code(), anchorColor.code())
                );
                double pairScore = harmony * anchorColor.weight() * candidateColor.weight();
                best = Math.max(best, pairScore);
            }
        }
        return best;
    }

    private List<WeightedColor> getWeightedColors(Clothes clothes) {
        return clothes.getSortedColorTags().stream()
                .map(tag -> new WeightedColor(
                        tag.getColorCode(),
                        tag.getColorRole() == ColorRole.PRIMARY
                                ? PRIMARY_COLOR_WEIGHT
                                : SECONDARY_COLOR_WEIGHT
                ))
                .toList();
    }

    private record WeightedColor(String code, double weight) {}

    /**
     * 스타일 Jaccard 유사도 기반 점수 (0.2 ~ 1.0).
     * 공통 스타일 태그가 많을수록 높습니다.
     */
    private double computeStyleScore(Clothes anchor, Clothes candidate) {
        Set<String> anchorStyles = getStyleCodeSet(anchor);
        Set<String> candidateStyles = getStyleCodeSet(candidate);

        if (anchorStyles.isEmpty() || candidateStyles.isEmpty()) {
            return 0.5;
        }

        long intersectionSize = anchorStyles.stream()
                .filter(candidateStyles::contains)
                .count();
        long unionSize = anchorStyles.size() + candidateStyles.size() - intersectionSize;

        double jaccard = unionSize == 0 ? 0.0 : (double) intersectionSize / unionSize;
        return 0.2 + 0.8 * jaccard;
    }

    /**
     * itemType(소분류) 코디 어울림 점수 (0.0 ~ 1.0).
     * {@link ItemTypeCompatibilityTable}의 cohesion group + 명시 페어를 사용합니다.
     */
    private double computeItemTypeScore(String anchorItemType, String candidateItemType) {
        return ItemTypeCompatibilityTable.score(anchorItemType, candidateItemType);
    }

    /**
     * 시즌 점수 (0.3 ~ 1.0).
     * 동일 시즌: 1.0 / 한쪽 null: 0.7 / 불일치: 0.3
     */
    private double computeSeasonScore(String anchorSeason, String candidateSeason) {
        if (!StringUtils.hasText(anchorSeason) || !StringUtils.hasText(candidateSeason)) {
            return 0.7;
        }
        return anchorSeason.equals(candidateSeason) ? 1.0 : 0.3;
    }

    // ── 변환 헬퍼 ─────────────────────────────────────────────────────────

    private AnchorItem toAnchorItem(WardrobeClothes wc) {
        Clothes clothes = wc.getClothes();
        String primaryColor = getPrimaryColorCode(clothes);
        return new AnchorItem(
                clothes.getId(),
                clothes.getName(),
                clothes.getImageUrl(),
                wc.getUserImageUrl(),
                clothes.getCategory(),
                clothes.getItemType(),
                primaryColor,
                toColorInfo(primaryColor)
        );
    }

    private ColorInfo toColorInfo(String colorCode) {
        if (!StringUtils.hasText(colorCode)) {
            return null;
        }
        try {
            ClothesColor color = ClothesColor.fromCode(colorCode);
            return new ColorInfo(color.name(), color.getLabel(), color.getHex());
        } catch (IllegalArgumentException exception) {
            return new ColorInfo(colorCode, colorCode, null);
        }
    }

    private String getPrimaryColorCode(Clothes clothes) {
        return clothes.getSortedColorTags().stream()
                .filter(c -> c.getColorRole() == ColorRole.PRIMARY)
                .findFirst()
                .map(ClothingColor::getColorCode)
                .orElse(null);
    }

    private List<String> getSecondaryColorCodes(Clothes clothes) {
        return clothes.getSortedColorTags().stream()
                .filter(c -> c.getColorRole() == ColorRole.SECONDARY)
                .map(ClothingColor::getColorCode)
                .toList();
    }

    private List<String> getStyleCodes(Clothes clothes) {
        return clothes.getSortedStyleTags().stream()
                .map(tag -> tag.getStyle().getCode())
                .toList();
    }

    private Set<String> getStyleCodeSet(Clothes clothes) {
        return clothes.getSortedStyleTags().stream()
                .map(tag -> tag.getStyle().getCode())
                .collect(Collectors.toSet());
    }
}
