package com.closetnangam.be.domain.recommendation.service;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.WeatherCompatibilityTable;
import com.closetnangam.be.domain.recommendation.dto.response.OotdResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import com.closetnangam.be.domain.wardrobe.service.WardrobeStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.closetnangam.be.domain.clothes.scoring.ColorCompatibilityTable.score;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OotdRecommendationService {

    private static final int MAX_CANDIDATES_PER_SLOT = 15;
    private static final int MAX_OUTER_CANDIDATES = 10;
    private static final int MAX_SHOES_CANDIDATES = 10;
    private static final int MIN_WARDROBE_THRESHOLD = 3;
    private static final int EXTERNAL_SUPPLEMENT_LIMIT = 30;
    private static final int MAX_COMBINATIONS = 6;

    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final WardrobeRepository wardrobeRepository;
    private final UserStyleRepository userStyleRepository;
    private final ClothesRepository clothesRepository;
    private final UserRepository userRepository;
    private final WardrobeStatisticsService wardrobeStatisticsService;

    @Transactional
    public OotdResponse recommend(Long currentUserId, Long wardrobeId, double currentTemp) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 추천 전 옷장 통계 기반 스타일 가중치 동기화
        wardrobeStatisticsService.getStatistics(currentUserId);

        ClothesGender userClothesGender = ClothesGender.fromUserGender(user.getGender());
        List<ClothesGender> allowedGenders = userClothesGender == ClothesGender.UNISEX
                ? List.of(ClothesGender.UNISEX)
                : List.of(ClothesGender.UNISEX, userClothesGender);

        Wardrobe wardrobe = wardrobeRepository.findById(wardrobeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옷장입니다."));

        if (!wardrobe.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인의 옷장만 추천받을 수 있습니다.");
        }

        ClothesSeason currentSeason = ClothesSeason.fromTemperature(currentTemp);
        List<WardrobeClothes> ownedClothes = wardrobeClothesRepository.findAllByWardrobeId(wardrobeId)
                .stream()
                .filter(wc -> wc.getOwnershipStatus() == OwnershipStatus.OWNED)
                .toList();

        List<UserStyle> userStyles = userStyleRepository.findAllByUserId(currentUserId);
        Map<String, Integer> styleWeights = userStyles.stream()
                .collect(Collectors.toMap(us -> us.getStyle().getCode(), UserStyle::getCombinedWeight));

        // 워드로브 clothes_id 목록 (외부 보충 시 중복 제외용)
        List<Long> wardrobeClothesIds = ownedClothes.stream()
                .map(wc -> wc.getClothes().getId())
                .toList();

        // 워드로브 WardrobeClothes Map (clothesId → WardrobeClothes, 응답 매핑용)
        Map<Long, WardrobeClothes> wardrobeMap = ownedClothes.stream()
                .collect(Collectors.toMap(wc -> wc.getClothes().getId(), wc -> wc));

        List<ScoredItem> tops = buildCandidates("TOP", ownedClothes, wardrobeClothesIds, wardrobeMap, currentTemp, currentSeason, styleWeights, allowedGenders);
        List<ScoredItem> bottoms = buildCandidates("BOTTOM", ownedClothes, wardrobeClothesIds, wardrobeMap, currentTemp, currentSeason, styleWeights, allowedGenders);
        List<ScoredItem> outers = buildCandidates("OUTER", ownedClothes, wardrobeClothesIds, wardrobeMap, currentTemp, currentSeason, styleWeights, allowedGenders)
                .stream().limit(MAX_OUTER_CANDIDATES).toList();
        List<ScoredItem> shoes = buildCandidates("SHOES", ownedClothes, wardrobeClothesIds, wardrobeMap, currentTemp, currentSeason, styleWeights, allowedGenders)
                .stream().limit(MAX_SHOES_CANDIDATES).toList();

        // TOP/BOTTOM 없으면 (드문 케이스지만) 빈 리스트 반환
        if (tops.isEmpty() || bottoms.isEmpty()) {
            return OotdResponse.builder()
                    .combinations(List.of())
                    .weatherLabel(buildWeatherLabel(currentSeason, currentTemp))
                    .currentTemp(currentTemp)
                    .build();
        }

        // 아우터 필수 여부 체크
        boolean requiresOuter = currentSeason.requiresOuter();
        List<ScoredItem> outerSlot = new ArrayList<>(outers);
        if (!requiresOuter) {
            // 아우터가 필수가 아닐 때, 아우터가 포함되지 않은 조합의 우선순위를 높이기 위해 null을 리스트 맨 앞에 추가
            outerSlot.add(0, null);
        } else if (outerSlot.isEmpty()) {
            outerSlot.add(null);
        }
        List<ScoredItem> shoeSlot = shoes.isEmpty() ? java.util.Collections.singletonList(null) : shoes;

        List<OotdResponse.OotdCombinationResponse> allCombinations = new ArrayList<>();
        for (ScoredItem top : tops) {
            for (ScoredItem bottom : bottoms) {
                for (ScoredItem outer : outerSlot) {
                    for (ScoredItem shoe : shoeSlot) {
                        if (requiresOuter && outer == null) continue;

                        // 아우터가 필수가 아닌데 아우터가 포함된 경우 패널티 부여 (아우터 없이 입는 것을 권장)
                        double outerPenalty = (!requiresOuter && outer != null) ? -5.0 : 0.0;

                        double total = top.score + bottom.score
                                + (outer != null ? outer.score : 0.0)
                                + (shoe != null ? shoe.score : 0.0)
                                + outerPenalty;
                        allCombinations.add(OotdResponse.OotdCombinationResponse.builder()
                                .top(mapToItemResponse(top))
                                .bottom(mapToItemResponse(bottom))
                                .outer(outer != null ? mapToItemResponse(outer) : null)
                                .shoes(shoe != null ? mapToItemResponse(shoe) : null)
                                .totalScore(Math.round(total * 10.0) / 10.0)
                                .build());
                    }
                }
            }
        }

        // 중복 방지 로직이 포함된 결과 선택
        // TOP+BOTTOM 조합당 최고 점수 1개만 남기기
        Map<String, OotdResponse.OotdCombinationResponse> bestByTopBottom = new LinkedHashMap<>();
        for (OotdResponse.OotdCombinationResponse combo : allCombinations) {
            String pair = combo.top().clothesId() + "_" + combo.bottom().clothesId();
            bestByTopBottom.merge(pair, combo, (a, b) -> a.totalScore() >= b.totalScore() ? a : b);
        }

        List<OotdResponse.OotdCombinationResponse> topCombinations = bestByTopBottom.values().stream()
                .sorted(Comparator.comparingDouble(OotdResponse.OotdCombinationResponse::totalScore).reversed())
                .limit(MAX_COMBINATIONS * 3) // 18개로 압축
                .collect(Collectors.toCollection(ArrayList::new));

        List<OotdResponse.OotdCombinationResponse> selected = pickDiverseCombinations(topCombinations, MAX_COMBINATIONS);

        return OotdResponse.builder()
                .combinations(selected)
                .weatherLabel(buildWeatherLabel(currentSeason, currentTemp))
                .currentTemp(currentTemp)
                .build();
    }

    /**
     * 전역 중복 방지 로직: 하나의 옷이 너무 많은 코디에 포함되지 않도록 함.
     */
    private List<OotdResponse.OotdCombinationResponse> pickDiverseCombinations(
            List<OotdResponse.OotdCombinationResponse> candidates, int limit) {
        List<OotdResponse.OotdCombinationResponse> result = new ArrayList<>();
        Set<Long> usedTopIds = new HashSet<>();

        for (OotdResponse.OotdCombinationResponse combo : candidates) {
            if (result.size() >= limit) break;
            Long topId = combo.top().clothesId();
            if (usedTopIds.contains(topId)) continue;
            result.add(combo);
            usedTopIds.add(topId);
        }

        // 부족하면 top 중복 허용해서 채움
        if (result.size() < limit) {
            for (OotdResponse.OotdCombinationResponse combo : candidates) {
                if (result.size() >= limit) break;
                if (!result.contains(combo)) {
                    result.add(combo);
                }
            }
        }

        Collections.shuffle(result);
        return result;
    }

    private boolean canAddWithoutExcessiveReuse(OotdResponse.OotdCombinationResponse combo, Map<Long, Integer> usage, int max) {
        if (usage.getOrDefault(combo.top().clothesId(), 0) >= max) return false;
        if (usage.getOrDefault(combo.bottom().clothesId(), 0) >= max) return false;
        if (combo.outer() != null && usage.getOrDefault(combo.outer().clothesId(), 0) >= max) return false;
        if (combo.shoes() != null && usage.getOrDefault(combo.shoes().clothesId(), 0) >= max) return false;
        return true;
    }

    private void updateUsageCount(OotdResponse.OotdCombinationResponse combo, Map<Long, Integer> usage) {
        usage.merge(combo.top().clothesId(), 1, Integer::sum);
        usage.merge(combo.bottom().clothesId(), 1, Integer::sum);
        if (combo.outer() != null) usage.merge(combo.outer().clothesId(), 1, Integer::sum);
        if (combo.shoes() != null) usage.merge(combo.shoes().clothesId(), 1, Integer::sum);
    }

    /**
     * 카테고리별 후보 구성.
     * 워드로브 OWNED 옷이 MIN_WARDROBE_THRESHOLD 미만이면 EXTERNAL_SHOPPING에서 보충.
     */
    private List<ScoredItem> buildCandidates(
            String category,
            List<WardrobeClothes> ownedClothes,
            List<Long> excludeIds,
            Map<Long, WardrobeClothes> wardrobeMap,
            double temp,
            ClothesSeason currentSeason,
            Map<String, Integer> styleWeights,
            List<ClothesGender> allowedGenders
    ) {
        List<WardrobeClothes> categoryOwned = ownedClothes.stream()
                .filter(wc -> wc.getClothes().getCategory().equals(category))
                .filter(wc -> allowedGenders == null || allowedGenders.contains(wc.getClothes().getGender()))
                .toList();

        List<Clothes> candidates = categoryOwned.stream()
                .map(WardrobeClothes::getClothes)
                .collect(Collectors.toCollection(ArrayList::new));

        // 워드로브 보유분이 부족하면 외부 데이터로 보충 (SHOES 포함)
        if (categoryOwned.size() < MIN_WARDROBE_THRESHOLD) {
            // MySQL은 빈 IN절(c.id not in ())을 허용하지 않으므로 dummy id로 방어
            List<Long> excludeForQuery = excludeIds.isEmpty() ? List.of(-1L) : excludeIds;
            List<Clothes> external = clothesRepository
                    .findExternalShoppingRecommendationCandidatesByCategory(
                            excludeForQuery,
                            category,
                            allowedGenders,
                            PageRequest.of(0, EXTERNAL_SUPPLEMENT_LIMIT)
                    );
            candidates.addAll(external);
        }

        return candidates.stream()
                .filter(c -> allowedGenders == null || allowedGenders.contains(c.getGender()))
                .map(c -> {
                    WardrobeClothes wc = wardrobeMap.get(c.getId());
                    return new ScoredItem(c, wc, score(c, wc, temp, currentSeason, styleWeights, allowedGenders));
                })
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(MAX_CANDIDATES_PER_SLOT + 10)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    java.util.Collections.shuffle(list);
                    return list.stream().limit(MAX_CANDIDATES_PER_SLOT).toList();
                }));}

    private double score(Clothes c, WardrobeClothes wc, double temp, ClothesSeason currentSeason, Map<String, Integer> styleWeights, List<ClothesGender> allowedGenders) {
        double weatherScore = WeatherCompatibilityTable.getWeatherScore(temp, c.getItemType());

        // 날씨 점수가 0이면 추천 후보에서 사실상 배제 (매우 낮은 점수 부여)
        if (weatherScore <= 0.0) {
            return -100.0;
        }

        // 성별 적합도 체크: 사용자의 성별과 맞지 않는 옷은 배제
        if (allowedGenders != null && !allowedGenders.contains(c.getGender())) {
            return -200.0;
        }

        boolean isCompatibleSeason = currentSeason.isCompatibleWith(c.getSeason());
        double seasonMatchScore = isCompatibleSeason ? 0.8 : 0.0;

        // 내 옷장의 옷일 경우 가중치 부여 (기본 1.2배, 즐겨찾기면 1.5배)
        double ownershipWeight = (wc != null) ? (wc.getFavorite() ? 1.5 : 1.2) : 1.0;

        double styleScore = 0.0;
        if (styleWeights != null && !styleWeights.isEmpty()) {
            styleScore = c.getSortedStyleTags().stream()
                    .map(st -> st.getStyle().getCode())
                    .mapToDouble(code -> {
                        Integer w = styleWeights.get(code);
                        // 가중치를 더 직접적으로 반영 (예: combinedWeight가 100이면 1.0점 추가)
                        // 온보딩 선호도가 높을수록 점수가 크게 상승하도록 조정
                        return w != null ? w / 50.0 : 0.0;
                    })
                    .max()
                    .orElse(0.0);
        }

        // 아우터 필수 기온일 때 아우터 카테고리라면 기온 적합도 비중을 높임
        double finalWeatherScore = weatherScore;
        if (currentSeason.requiresOuter() && "OUTER".equals(c.getCategory())) {
            finalWeatherScore *= 1.2;
        }

        // 전체 점수에서 스타일 비중을 날씨만큼이나 중요하게 처리
        return (finalWeatherScore * ownershipWeight) + (styleScore * 1.5) + seasonMatchScore;
    }

    private OotdResponse.OotdItemResponse mapToItemResponse(ScoredItem item) {
        Clothes c = item.clothes();
        WardrobeClothes wc = item.wardrobeClothes();
        String primaryColor = c.getSortedColorTags().stream()
                .findFirst()
                .map(cc -> cc.getColorCode())
                .orElse(null);
        String imageUrl = (wc != null && wc.getUserImageUrl() != null && !wc.getUserImageUrl().isBlank())
                ? wc.getUserImageUrl()
                : c.getImageUrl();

        return OotdResponse.OotdItemResponse.builder()
                .clothesId(c.getId())
                .wardrobeClothesId(wc != null ? wc.getId() : null)
                .name(c.getName())
                .brand(c.getBrandName())
                .color(primaryColor)
                .imageUrl(imageUrl)
                .externalProductUrl(c.getExternalProductUrl())
                .category(c.getCategory())
                .itemType(c.getItemType())
                .favorite(wc != null && wc.getFavorite())
                .build();
    }

    private String buildWeatherLabel(ClothesSeason season, double temp) {
        return switch (season) {
            case SUMMER -> "오늘 %.0f°C — 반팔·반바지 추천".formatted(temp);
            case SPRING -> "오늘 %.0f°C — 얇은 셔츠·면바지 추천".formatted(temp);
            case FALL   -> "오늘 %.0f°C — 가디건·자켓 추천".formatted(temp);
            case WINTER -> "오늘 %.0f°C — 패딩·방한 필수".formatted(temp);
            default     -> "오늘 %.0f°C — 사계절 코디 추천".formatted(temp);
        };
    }

    // Clothes + WardrobeClothes(nullable) 묶음
    private record ScoredItem(Clothes clothes, WardrobeClothes wardrobeClothes, double score) {}
}