package com.closetnangam.be.domain.recommendation.service;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.WeatherCompatibilityTable;
import com.closetnangam.be.domain.recommendation.dto.response.OotdResponse;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OotdRecommendationService {

    private static final int MAX_CANDIDATES_PER_SLOT = 3;

    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final WardrobeRepository wardrobeRepository;
    private final UserStyleRepository userStyleRepository;

    public OotdResponse recommend(Long currentUserId, Long wardrobeId, double currentTemp) {
        Wardrobe wardrobe = wardrobeRepository.findById(wardrobeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옷장입니다."));

        if (!wardrobe.getUser().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 옷장만 추천받을 수 있습니다.");
        }

        ClothesSeason currentSeason = ClothesSeason.fromTemperature(currentTemp);
        List<WardrobeClothes> ownedClothes = wardrobeClothesRepository.findAllByWardrobeId(wardrobeId)
                .stream()
                .filter(wc -> wc.getOwnershipStatus() == OwnershipStatus.OWNED)
                .toList();

        List<UserStyle> userStyles = userStyleRepository.findAllByUserId(currentUserId);
        Map<String, Integer> styleWeights = userStyles.stream()
                .collect(Collectors.toMap(us -> us.getStyle().getCode(), UserStyle::getCombinedWeight));

        List<ScoredItem> tops = filterAndScore(ownedClothes, "TOP", currentTemp, styleWeights);
        List<ScoredItem> bottoms = filterAndScore(ownedClothes, "BOTTOM", currentTemp, styleWeights);
        List<ScoredItem> outers = filterAndScore(ownedClothes, "OUTER", currentTemp, styleWeights);

        if (tops.isEmpty() || bottoms.isEmpty() || (currentSeason.requiresOuter() && outers.isEmpty())) {
            return OotdResponse.builder()
                    .combinations(List.of())
                    .weatherLabel(buildWeatherLabel(currentSeason, currentTemp))
                    .currentTemp(currentTemp)
                    .build();
        }

        List<OotdResponse.OotdCombinationResponse> combinations = new ArrayList<>();

        if (currentSeason.requiresOuter()) {
            for (ScoredItem top : tops) {
                for (ScoredItem bottom : bottoms) {
                    for (ScoredItem outer : outers) {
                        combinations.add(OotdResponse.OotdCombinationResponse.builder()
                                .top(mapToItemResponse(top.item))
                                .bottom(mapToItemResponse(bottom.item))
                                .outer(mapToItemResponse(outer.item))
                                .totalScore(Math.round((top.score + bottom.score + outer.score) * 10.0) / 10.0)
                                .build());
                    }
                }
            }
        } else {
            for (ScoredItem top : tops) {
                for (ScoredItem bottom : bottoms) {
                    combinations.add(OotdResponse.OotdCombinationResponse.builder()
                            .top(mapToItemResponse(top.item))
                            .bottom(mapToItemResponse(bottom.item))
                            .totalScore(Math.round((top.score + bottom.score) * 10.0) / 10.0)
                            .build());
                }
            }
        }

        List<OotdResponse.OotdCombinationResponse> top10 = combinations.stream()
                .sorted(Comparator.comparingDouble(OotdResponse.OotdCombinationResponse::totalScore).reversed())
                .limit(10)
                .toList();

        return OotdResponse.builder()
                .combinations(top10)
                .weatherLabel(buildWeatherLabel(currentSeason, currentTemp))
                .currentTemp(currentTemp)
                .build();
    }

    private String buildWeatherLabel(ClothesSeason season, double temp) {
        return switch (season) {
            case SUMMER    -> "오늘 %.0f°C — 반팔·반바지 추천".formatted(temp);
            case SPRING    -> "오늘 %.0f°C — 얇은 셔츠·면바지 추천".formatted(temp);
            case FALL      -> "오늘 %.0f°C — 가디건·자켓 추천".formatted(temp);
            case WINTER    -> "오늘 %.0f°C — 패딩·방한 필수".formatted(temp);
            default        -> "오늘 %.0f°C — 사계절 코디 추천".formatted(temp);
        };
    }

    private List<ScoredItem> filterAndScore(List<WardrobeClothes> items, String targetCategory, double temp, Map<String, Integer> styleWeights) {
        ClothesSeason currentSeason = ClothesSeason.fromTemperature(temp);
        return items.stream()
                .filter(wc -> wc.getClothes().getCategory().equals(targetCategory))
                .map(wc -> {
                    double weatherScore = WeatherCompatibilityTable.getWeatherScore(temp, wc.getClothes().getItemType());

                    // 계절 일치 가점 (0.0 ~ 0.5)
                    double seasonMatchScore = currentSeason.isCompatibleWith(wc.getClothes().getSeason()) ? 0.5 : 0.0;
                    double favoriteWeight = wc.getFavorite() ? 1.5 : 1.0;
                    double styleScore = 0.0;
                    // 의상의 스타일 태그들 중 유저 선호 스타일과 일치하는 최대 combined_weight 반영
                    List<String> itemStyleCodes = wc.getClothes().getSortedStyleTags().stream()
                            .map(st -> st.getStyle().getCode())
                            .toList();
                    for (String code : itemStyleCodes) {
                        Integer weight = styleWeights.get(code);
                        if (weight != null) {
                            styleScore = Math.max(styleScore, weight / 100.0);
                        }
                    }

                    double totalItemScore = (weatherScore * favoriteWeight) + styleScore + seasonMatchScore;
                    return new ScoredItem(wc, totalItemScore);
                })
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(MAX_CANDIDATES_PER_SLOT)
                .toList();
    }

    private OotdResponse.OotdItemResponse mapToItemResponse(WardrobeClothes wc) {
        Clothes c = wc.getClothes();
        String primaryColor = c.getSortedColorTags().stream()
                .findFirst()
                .map(cc -> cc.getColorCode())
                .orElse(null);

        return OotdResponse.OotdItemResponse.builder()
                .clothesId(c.getId())
                .wardrobeClothesId(wc.getId())
                .name(c.getName())
                .brand(c.getBrandName())
                .color(primaryColor)
                .imageUrl(wc.getUserImageUrl())
                .externalProductUrl(c.getExternalProductUrl())
                .category(c.getCategory())
                .itemType(c.getItemType())
                .favorite(wc.getFavorite())
                .build();
    }

    private record ScoredItem(WardrobeClothes item, double score) {}
}
