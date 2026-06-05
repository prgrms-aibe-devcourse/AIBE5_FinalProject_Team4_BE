package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.TemperatureRange;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot;
import com.closetnangam.be.domain.clothes.scoring.ColorCompatibilityTable;
import com.closetnangam.be.domain.clothes.scoring.ItemTypeCompatibilityTable;
import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.domain.recommendation.scoring.WeatherCompatibilityTable;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleProductRecommender {

    private static final int MAX_RESULTS = 20;
    private static final double STYLE_WEIGHT = 0.6d;
    private static final double WEATHER_WEIGHT = 0.4d;

    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final WardrobeRepository wardrobeRepository;

    public List<RecommendResponse> recommendByStyle(Long wardrobeId) {
        return recommendByStyle(wardrobeId, 20.0d);
    }

    public List<RecommendResponse> recommendByStyle(Long wardrobeId, double currentTemp) {
        Wardrobe wardrobe = wardrobeRepository.findById(wardrobeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옷장입니다."));

        List<WardrobeClothes> wardrobeItems = wardrobeClothesRepository.findAllByWardrobeId(wardrobeId);

        WardrobeProfile wardrobeProfile = extractProfile(wardrobe, wardrobeItems);

        Set<Long> excludedClothesIds = wardrobeItems.stream()
                .map(WardrobeClothes::getClothes)
                .filter(Objects::nonNull)
                .map(Clothes::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Clothes> candidates = clothesRepository.findAllForRecommendation(PageRequest.of(0, 100));

        log.info(
                "Style recommendation processed. wardrobeId={}, currentTemp={}, candidatesFound={}",
                wardrobeId, currentTemp, candidates.size()
        );

        if (candidates.isEmpty()) {
            log.warn("No candidates found in DB for recommendation.");
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(clothes -> clothes.getId() != null)
                .filter(clothes -> !excludedClothesIds.contains(clothes.getId()))
                .map(clothes -> scoreCandidate(wardrobeProfile, clothes, currentTemp))
                .sorted(Comparator.comparingDouble(ScoredRecommendation::score)
                        .reversed()
                )
                .limit(MAX_RESULTS)
                .map(this::mapToRecommendResponse)
                .toList();
    }

    private RecommendResponse mapToRecommendResponse(ScoredRecommendation scored) {
        Clothes clothes = scored.clothes();
        return new RecommendResponse(
                clothes.getName(),
                clothes.getExternalProductUrl(),
                clothes.getImageUrl(),
                "0", // 가격 정보는 엔티티에 직접 없을 수 있음
                String.format("%.2f", scored.score()),
                scored.reason()
        );
    }

//    todo // 현재: 옷장 순회로 그때그때 계산 (USER_STYLES 미사용) 온보딩 완성 후 교체할 부분
    private WardrobeProfile extractProfile(Wardrobe wardrobe, List<WardrobeClothes> wardrobeItems) {
        if (wardrobeItems.isEmpty()) {
            User user = wardrobe.getUser();
            String defaultItemType = user.getDefaultAnchorItemType();
            return new WardrobeProfile("CASUAL", null, defaultItemType);
        }
        // [1] 가중치 맵 합계 산출 (단일 패스)
        Map<String, Double> styleWeights = new HashMap<>();
        Map<String, Double> colorWeights = new HashMap<>();
        Map<String, Double> itemTypeWeights = new HashMap<>();

        for (WardrobeClothes wc : wardrobeItems) {
            double weight = wc.getFavorite() ? 2.0d : 1.0d;
            ClothesTagSnapshot snapshot = wc.getClothes().getRecommendationTagSnapshot();

            // Style Tags
            for (String styleCode : snapshot.styleCodes()) {
                styleWeights.merge(styleCode, weight, Double::sum);
            }

            // Colors
            if (snapshot.primaryColor() != null) {
                colorWeights.merge(snapshot.primaryColor(), weight, Double::sum);
            }

            // Item Type
            String itemType = wc.getClothes().getItemType();
            if (itemType != null) {
                itemTypeWeights.merge(itemType, weight, Double::sum);
            }
        }

        String anchorStyle = styleWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("CASUAL");

        String anchorColor = colorWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String anchorItemType = itemTypeWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new WardrobeProfile(anchorStyle, anchorColor, anchorItemType);
    }

    private ScoredRecommendation scoreCandidate(WardrobeProfile wardrobeProfile, Clothes clothes, double currentTemp) {
        ClothesTagSnapshot snapshot = clothes.getRecommendationTagSnapshot();

        // 1. 스타일 점수 (60%) - 사용자 취향 스타일과 매칭
        // ItemTypeCompatibilityTable.score는 anchorItemType과 candidateItemType을 기대함.
        // 하지만 현재 STYLE_WEIGHT로 사용되므로, 스타일 코드 간의 호환성을 체크하는 것이 의도임.
        double styleScore = ItemTypeCompatibilityTable.score(
                wardrobeProfile.anchorPrimaryStyle(),
                snapshot.primaryStyleCode() != null ? snapshot.primaryStyleCode() : "CASUAL"
        );

        // 2. 날씨 점수 (40%) - 현재 기온에 따른 아이템 타입 적합도
        double weatherScore = WeatherCompatibilityTable.getWeatherScore(currentTemp, clothes.getItemType());

        // 최종 점수 계산 (가중치 합산)
        double totalScore = (STYLE_WEIGHT * styleScore) + (WEATHER_WEIGHT * weatherScore);

        String reason = String.format("Style: %.1f, Weather: %.1f",
                styleScore, weatherScore);

        log.debug("Scoring candidate: name={}, totalScore={}, reason={}",
                clothes.getName(), totalScore, reason);

        return new ScoredRecommendation(clothes, totalScore, reason);
    }

    private String resolvePrimaryColor(ClothesTagSnapshot snapshot) {
        if (snapshot.primaryColor() != null && !snapshot.primaryColor().isEmpty()) return snapshot.primaryColor();
        if (snapshot.weightedColors() != null && !snapshot.weightedColors().isEmpty()) return snapshot.weightedColors().get(0).code();
        return "WHITE";
    }

    private record ScoredRecommendation(Clothes clothes, double score, String reason) {}

    private record WardrobeProfile(String anchorPrimaryStyle, String anchorPrimaryColor, String anchorItemType) {}
}
