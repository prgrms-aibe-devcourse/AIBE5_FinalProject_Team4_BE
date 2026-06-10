package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.AnchorItem;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.ColorInfo;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRecommendationResponse.RecommendedItem;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.SeasonType;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot.WeightedColor;
import com.closetnangam.be.domain.clothes.scoring.ColorCompatibilityTable;
import com.closetnangam.be.domain.clothes.scoring.ItemTypeCompatibilityTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesRecommendationService {

    private static final String CLOTHES_NOT_FOUND_MESSAGE = "해당 옷을 찾을 수 없습니다.";
    private static final int CANDIDATE_LIMIT_PER_CATEGORY = 500;
    private static final String EXTERNAL_DEFAULT_SEASON = "ALL_SEASON";

    /**
     * 응답에서 카테고리를 보여줄 순서.
     * anchor 카테고리가 CATEGORY_ORDER 내에 있을 경우 최대 3개, 그 외(예: ACCESSORY)일 경우 최대 4개 카테고리가 반환됩니다.
     */
    private static final List<String> CATEGORY_ORDER = List.of("TOP", "BOTTOM", "OUTER", "SHOES");

    /** scoredAndGrouped() pre-filter용 Set — CATEGORY_ORDER 외 후보는 채점에서 제외 */
    private static final Set<String> CATEGORY_SET = Set.copyOf(CATEGORY_ORDER);

    private static final ColorInfo UNKNOWN_COLOR_INFO = new ColorInfo("UNKNOWN", "UNKNOWN", null);

    /**
     * 색상 코드 → ColorInfo 정적 캐시.
     * ClothesColor 는 변경되지 않는 정적 데이터이므로 클래스 로드 시점에 한 번만 구성합니다.
     */
    private static final Map<String, ColorInfo> COLOR_INFO_CACHE = Arrays.stream(ClothesColor.values())
            .collect(Collectors.toUnmodifiableMap(
                    ClothesColor::name,
                    c -> new ColorInfo(c.name(), c.getLabel(), c.getHex())
            ));

    /** 이미 DEBUG 로그를 출력한 미등록 색상 코드 — 동일 코드의 반복 로깅을 방지합니다. */
    private final Set<String> loggedUnknownColorCodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 점수 가중치 (합계 = 1.0)
    private static final double WEIGHT_COLOR     = 0.35;
    private static final double WEIGHT_STYLE     = 0.30;
    private static final double WEIGHT_SEASON    = 0.15;
    private static final double WEIGHT_ITEM_TYPE = 0.20;

    /** 색상·스타일 데이터 부재 시 중립 점수 */
    private static final double SCORE_NEUTRAL = 0.5;
    /** 시즌 한쪽 미입력 시 점수 */
    private static final double SCORE_SEASON_UNKNOWN = 0.7;
    /** 시즌 불일치 시 점수 */
    private static final double SCORE_SEASON_MISMATCH = 0.3;
    /** 스타일 불일치 시 점수 */
    private static final double SCORE_STYLE_MISMATCH = 0.2;

    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesRepository clothesRepository;

    /**
     * 기준 옷({clothesId})와 어울리는 상품을 카테고리별로 추천합니다.
     *
     * <p>후보 풀: {@code CLOTHES} 중 {@code clothes_info_source = EXTERNAL_SHOPPING} 공용 쇼핑 마스터만.
     * 기준 옷과 같은 카테고리를 제외하고 TOP/BOTTOM/OUTER/SHOES 각각 최대 500건(최신순)을 조회한 뒤
     * 점수 상위를 반환합니다. PHOTO·PURCHASE_HISTORY 등 개인 등록 마스터는 후보에 포함하지 않습니다.
     *
     * <p><b>Precondition:</b> 호출 전에 컨트롤러에서 {@code SecurityUtils.verifyUserIdMatch(userId)}로
     * JWT 사용자 일치를 검증해야 합니다.
     *
     * @param userId           인증된 사용자 ID (컨트롤러에서 JWT 검증 후 전달)
     * @param clothesId        기준 옷 ID
     * @param limitPerCategory 카테고리당 최대 추천 수 (1~10, 컨트롤러에서 검증)
     * @throws NoSuchElementException {@code clothesId}가 존재하지 않거나 {@code userId} 소유가 아닌 경우 (HTTP 404)
     */
    public ClothesRecommendationResponse recommend(Long userId, Long clothesId, int limitPerCategory) {
        // userId와 clothesId를 함께 조회해 해당 옷이 이 사용자 소유임을 확인합니다.
        WardrobeClothes anchor = wardrobeClothesRepository
                .findByClothesIdAndUserId(clothesId, userId)
                .orElseThrow(() -> new NoSuchElementException(CLOTHES_NOT_FOUND_MESSAGE));

        // join fetch 로 Clothes 가 반드시 로딩되어야 합니다. null 이면 데이터 정합성 이상입니다.
        Clothes anchorClothes = anchor.getClothes();
        if (anchorClothes == null) {
            throw new IllegalStateException("기준 옷의 Clothes 관계를 로딩할 수 없습니다. wardrobeClothesId=" + anchor.getId());
        }

        String excludeCategory = anchorClothes.getCategory();
        Set<Long> ownedClothesIds = wardrobeClothesRepository.findOwnedClothesIdsByUserId(userId, OwnershipStatus.OWNED)
                .stream()
                .collect(Collectors.toSet());

        List<ScoringCandidate> candidates = loadCandidates(excludeCategory, ownedClothesIds);

        AnchorScoringContext anchorContext = AnchorScoringContext.from(anchor);
        Map<String, List<RecommendedItem>> recommendations = scoredAndGrouped(anchorContext, candidates, limitPerCategory);

        return new ClothesRecommendationResponse(toAnchorItem(anchor, anchorClothes, anchorContext.tagSnapshot()), recommendations);
    }

    private List<ScoringCandidate> loadCandidates(String excludeCategory, Set<Long> ownedClothesIds) {
        /*
         * 카테고리별로 DB를 분리 조회합니다 (요청당 최대 CATEGORY_ORDER.size()-1 회, 각 CANDIDATE_LIMIT_PER_CATEGORY 건).
         *
         * 의도: 단일 쿼리(findExternalCandidatesForComplementaryRecommendation)는 LIMIT 500을 전체에 적용해
         * 한 카테고리(예: TOP)에 후보가 쏠릴 수 있습니다. 카테고리마다 최신 N건을 보장하려면
         * 카테고리별 페이징이 필요합니다.
         *
         * 트레이드오프: anchor 제외 시 최대 3×500=1500건 후보 로드. 트래픽 증가 시
         * CANDIDATE_LIMIT_PER_CATEGORY 조정 또는 캐시를 검토하세요.
         *
         * 태그(style/color)는 Clothes @Fetch(SUBSELECT)로 채점 시점에 일괄 로딩됩니다.
         */
        List<ScoringCandidate> candidates = new ArrayList<>();
        for (String category : CATEGORY_ORDER) {
            if (category.equals(excludeCategory)) {
                continue;
            }
            clothesRepository.findComplementaryRecommendationCandidatesByCategory(
                            category,
                            PageRequest.of(0, CANDIDATE_LIMIT_PER_CATEGORY)
                    )
                    .stream()
                    .filter(clothes -> clothes.getId() != null && !ownedClothesIds.contains(clothes.getId()))
                    .map(clothes -> new ScoringCandidate(
                            clothes,
                            clothes.getRecommendationTagSnapshot(),
                            resolveCandidateSeason(clothes),
                            null,
                            null
                    ))
                    .forEach(candidates::add);
        }
        return candidates;
    }

    // ── 점수 계산 및 카테고리별 그룹화 ────────────────────────────────────

    private Map<String, List<RecommendedItem>> scoredAndGrouped(
            AnchorScoringContext anchorContext,
            List<ScoringCandidate> candidates,
            int limit
    ) {
        Map<String, List<ScoringCandidate>> candidatesByCategory = new HashMap<>();
        for (ScoringCandidate candidate : candidates) {
            Clothes clothes = candidate.clothes();
            if (clothes == null) {
                continue;
            }
            String category = clothes.getCategory();
            if (!CATEGORY_SET.contains(category)) {
                continue;
            }
            if (candidate.tagSnapshot() == null) {
                log.debug("Clothes(id={})의 태그 스냅샷을 생성할 수 없습니다 — 후보에서 제외합니다.", clothes.getId());
                continue;
            }
            if (isSummerWinterSeasonClash(anchorContext.season(), candidate.season(), clothes.getItemType())) {
                continue;
            }
            candidatesByCategory
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(candidate);
        }

        Map<String, List<RecommendedItem>> ordered = new LinkedHashMap<>();
        for (String category : CATEGORY_ORDER) {
            List<ScoringCandidate> categoryCandidates = candidatesByCategory.getOrDefault(category, List.of());
            if (categoryCandidates.isEmpty()) {
                continue;
            }

            List<RecommendedItem> sorted = categoryCandidates.stream()
                    .map(entry -> toRecommendedItem(anchorContext, entry))
                    .sorted(Comparator.comparingInt(RecommendedItem::compatibilityScore).reversed())
                    .limit(limit)
                    .toList();
            ordered.put(category, sorted);
        }
        return ordered;
    }

    private RecommendedItem toRecommendedItem(AnchorScoringContext anchorContext, ScoringCandidate entry) {
        Clothes candidateClothes = entry.clothes();
        ClothesTagSnapshot candidateTags = entry.tagSnapshot();

        double colorScore = computeColorScore(anchorContext.tagSnapshot().weightedColors(), candidateTags.weightedColors());
        double styleScore = computeStyleScore(anchorContext.tagSnapshot().primaryStyleCode(), candidateTags.styleCodes());
        double seasonScore = computeSeasonScore(anchorContext.season(), entry.season());
        double itemTypeScore = computeItemTypeScore(anchorContext.itemType(), candidateClothes.getItemType());

        int total = (int) Math.round(
                100.0 * (
                        WEIGHT_COLOR * colorScore
                                + WEIGHT_STYLE * styleScore
                                + WEIGHT_SEASON * seasonScore
                                + WEIGHT_ITEM_TYPE * itemTypeScore
                )
        );

        return new RecommendedItem(
                candidateClothes.getId(),
                entry.wardrobeClothesId(),
                candidateClothes.getName(),
                candidateClothes.getBrandName(),
                candidateClothes.getImageUrl(),
                entry.userImageUrl(),
                candidateClothes.getCategory(),
                candidateClothes.getItemType(),
                candidateTags.primaryColor(),
                toColorInfo(candidateTags.primaryColor()),
                candidateTags.secondaryColorCodes(),
                candidateTags.styleCodes(),
                entry.season(),
                total,
                candidateClothes.getGender().name(),
                candidateClothes.getExternalProductUrl()
        );
    }

    // ── 개별 점수 계산 ────────────────────────────────────────────────────

    /**
     * 색상 어울림 점수 (0.0 ~ 1.0).
     * 양쪽 옷의 primary·secondary 색상 조합을 양방향으로 비교합니다.
     *
     * <p>조기 탈출 최적화를 위해 {@code anchorColors}, {@code candidateColors} 모두
     * weight 내림차순 정렬된 목록이어야 합니다
     * ({@link ClothesTagSnapshot#weightedColors()} 반환값).
     *
     * @param anchorColors    weight DESC 정렬된 anchor 색상 목록
     * @param candidateColors weight DESC 정렬된 candidate 색상 목록
     */
    private double computeColorScore(List<WeightedColor> anchorColors, List<WeightedColor> candidateColors) {
        if (anchorColors == null || anchorColors.isEmpty() || candidateColors == null || candidateColors.isEmpty()) {
            return SCORE_NEUTRAL;
        }

        double best = 0.0;
        for (WeightedColor anchorColor : anchorColors) {
            // weightedColors는 weight DESC 정렬이므로, 현재 anchorWeight로 만들 수 있는 최대 pairScore
            // (= anchorWeight × maxHarmony × maxCandidateWeight = anchorWeight × 1.0 × 1.0)가
            // 이미 best 이하라면 이후 anchor 항목 전부 무의미 → 외부 루프 조기 탈출
            if (anchorColor.weight() <= best) {
                break;
            }
            for (WeightedColor candidateColor : candidateColors) {
                // candidateColors 도 weight DESC 정렬이므로, anchorWeight × candidateWeight ≤ best 이면
                // 이후 후보는 harmony=1.0 이어도 best 를 초과할 수 없음 → 내부 루프 조기 탈출
                if (anchorColor.weight() * candidateColor.weight() <= best) {
                    break;
                }
                double harmony = ColorCompatibilityTable.bestHarmony(anchorColor.code(), candidateColor.code());
                double pairScore = harmony * anchorColor.weight() * candidateColor.weight();
                if (pairScore > best) {
                    best = pairScore;
                    if (best >= 1.0) {
                        return best;
                    }
                }
            }
        }
        return best;
    }

    /**
     * 스타일 점수 (0.2 ~ 1.0).
     * 기준 옷 PRIMARY 스타일이 후보 옷의 PRIMARY·SECONDARY 태그 중 하나라도 포함되면 1.0.
     * anchor PRIMARY 스타일이 없으면 중립(0.5), 후보 스타일 태그가 없으면 불일치(0.2).
     */
    private double computeStyleScore(String anchorPrimaryStyle, List<String> candidateStyles) {
        if (!StringUtils.hasText(anchorPrimaryStyle)) {
            return SCORE_NEUTRAL;
        }
        if (candidateStyles == null || candidateStyles.isEmpty()) {
            return SCORE_STYLE_MISMATCH;
        }
        return candidateStyles.contains(anchorPrimaryStyle) ? 1.0 : SCORE_STYLE_MISMATCH;
    }

    /**
     * itemType(소분류) 코디 어울림 점수 (0.0 ~ 1.0).
     * 한쪽이라도 itemType이 null이거나 공백이면 중립 점수를 반환합니다.
     */
    private double computeItemTypeScore(String anchorItemType, String candidateItemType) {
        if (!StringUtils.hasText(anchorItemType) || !StringUtils.hasText(candidateItemType)) {
            return SCORE_NEUTRAL;
        }
        return ItemTypeCompatibilityTable.score(anchorItemType, candidateItemType);
    }

    /**
     * 시즌 점수.
     * 반환 타입은 {@code double}이지만, 아래 세 값 중 하나만 반환합니다 (연속 범위가 아님).
     * <ul>
     *   <li>동일 시즌: 1.0</li>
     *   <li>한쪽 미입력: {@link #SCORE_SEASON_UNKNOWN} (0.7)</li>
     *   <li>불일치: {@link #SCORE_SEASON_MISMATCH} (0.3)</li>
     * </ul>
     */
    private double computeSeasonScore(String anchorSeason, String candidateSeason) {
        if (!StringUtils.hasText(anchorSeason) || !StringUtils.hasText(candidateSeason)) {
            return SCORE_SEASON_UNKNOWN;
        }
        return anchorSeason.equals(candidateSeason) ? 1.0 : SCORE_SEASON_MISMATCH;
    }

    /**
     * 여름(HOT) ↔ 겨울(COLD) 조합은 추천 후보에서 제외합니다.
     * ALL/ALL_SEASON·미입력·봄가을(MILD) 등은 기존 점수 로직만 적용합니다.
     */
    private boolean isSummerWinterSeasonClash(String anchorSeason, String candidateSeason, String candidateItemType) {
        Optional<SeasonType> anchorType = resolveSeasonType(anchorSeason);
        Optional<SeasonType> candidateType = resolveSeasonType(candidateSeason)
                .or(() -> ItemTypeCompatibilityTable.inferSeasonTypeFromItemType(candidateItemType));

        if (anchorType.isEmpty() || candidateType.isEmpty()) {
            return false;
        }
        return (anchorType.get() == SeasonType.HOT && candidateType.get() == SeasonType.COLD)
                || (anchorType.get() == SeasonType.COLD && candidateType.get() == SeasonType.HOT);
    }

    private static Optional<SeasonType> resolveSeasonType(String season) {
        if (!StringUtils.hasText(season)) {
            return Optional.empty();
        }
        String normalized = season.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "ALL_SEASON".equals(normalized)) {
            return Optional.empty();
        }

        List<SeasonType> matched = Arrays.stream(SeasonType.values())
                .filter(type -> type.matches(season))
                .toList();
        if (matched.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(matched.get(0));
    }

    // ── 변환 헬퍼 ─────────────────────────────────────────────────────────

    private AnchorItem toAnchorItem(WardrobeClothes wc, Clothes clothes, ClothesTagSnapshot tagSnapshot) {
        return new AnchorItem(
                clothes.getId(),
                clothes.getName(),
                clothes.getImageUrl(),
                wc.getUserImageUrl(),
                clothes.getCategory(),
                clothes.getItemType(),
                tagSnapshot.primaryColor(),
                toColorInfo(tagSnapshot.primaryColor())
        );
    }

    private ColorInfo toColorInfo(String colorCode) {
        if (!StringUtils.hasText(colorCode)) {
            return UNKNOWN_COLOR_INFO;
        }
        ColorInfo cached = COLOR_INFO_CACHE.get(colorCode);
        if (cached == null) {
            if (loggedUnknownColorCodes.add(colorCode)) {
                log.debug("등록되지 않은 색상 코드가 발견되었습니다 — DB 데이터를 확인하세요.");
            }
            return UNKNOWN_COLOR_INFO;
        }
        return cached;
    }

    private static String resolveCandidateSeason(Clothes clothes) {
        if (clothes.getSeason() == null) {
            return EXTERNAL_DEFAULT_SEASON;
        }
        return clothes.getSeason().name();
    }

    private record ScoringCandidate(
            Clothes clothes,
            ClothesTagSnapshot tagSnapshot,
            String season,
            Long wardrobeClothesId,
            String userImageUrl
    ) {
    }

    private record AnchorScoringContext(
            ClothesTagSnapshot tagSnapshot,
            String itemType,
            String season
    ) {
        /** @param anchor {@code getClothes()} 가 null 이 아님을 호출자가 보장해야 합니다. */
        static AnchorScoringContext from(WardrobeClothes anchor) {
            Clothes clothes = anchor.getClothes();
            return new AnchorScoringContext(
                    clothes.getRecommendationTagSnapshot(),
                    clothes.getItemType(),
                    resolveCandidateSeason(clothes)
            );
        }
    }
}
