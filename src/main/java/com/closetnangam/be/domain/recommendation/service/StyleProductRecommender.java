package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot;
import com.closetnangam.be.domain.clothes.scoring.WeatherCompatibilityTable;
import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.domain.recommendation.entity.RecommendationFeedback;
import com.closetnangam.be.domain.recommendation.enums.FeedbackType;
import com.closetnangam.be.domain.recommendation.repository.RecommendationFeedbackRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.entity.UserStyle;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import com.closetnangam.be.domain.wardrobe.service.WardrobeStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleProductRecommender {

    private static final int MAX_RESULTS = 20;
    private static final int CANDIDATE_LIMIT = 500;
    private static final double STYLE_WEIGHT = 0.6d;
    private static final double WEATHER_WEIGHT = 0.4d;

    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final WardrobeRepository wardrobeRepository;
    private final UserStyleRepository userStyleRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final WardrobeStatisticsService wardrobeStatisticsService;

    @Transactional
    public List<RecommendResponse> recommendByStyle(Long currentUserId, Long wardrobeId, double currentTemp) {
        wardrobeStatisticsService.getStatistics(currentUserId);
        Wardrobe wardrobe = wardrobeRepository.findById(wardrobeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옷장입니다."));

        if (!wardrobe.getUser().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("본인의 옷장만 추천받을 수 있습니다.");
        }

        // [1] 사용자 스타일 점수 로드
        List<UserStyle> userStyles = userStyleRepository.findAllByUserId(currentUserId);

        // [2] 추천 제외 목록 로드
        Set<Long> excludedSet = new HashSet<>();

        // 이미 보유한 옷 제외
        List<WardrobeClothes> wardrobeItems = wardrobeClothesRepository.findAllByWardrobeId(wardrobeId);
        wardrobeItems.stream()
                .map(WardrobeClothes::getClothes)
                .filter(Objects::nonNull)
                .map(Clothes::getId)
                .forEach(excludedSet::add);

        // 추천 제외(EXCLUDE) 피드백 옷 제외
        recommendationFeedbackRepository.findAllByUserId(currentUserId).stream()
                .filter(RecommendationFeedback::isExcluded)
                .map(feedback -> feedback.getClothes().getId())
                .forEach(excludedSet::add);

        // [3] 후보군 로드
        List<Clothes> candidates = clothesRepository.findAllForRecommendation(PageRequest.of(0, CANDIDATE_LIMIT));

        log.info(
                "Style recommendation processed. userId={}, wardrobeId={}, currentTemp={}, candidatesFound={}",
                currentUserId, wardrobeId, currentTemp, candidates.size()
        );

// [4] 점수 계산 및 필터링
        User.Gender userGender = wardrobe.getUser().getGender();

        List<ScoredRecommendation> scoredRecommendations = new ArrayList<>();
        for (Clothes clothes : candidates) {
            if (clothes == null || clothes.getId() == null || excludedSet.contains(clothes.getId())) {
                continue;
            }
            // 성별 필터링 추가
            ClothesGender clothesGender = clothes.getGender();
            if (clothesGender != null && clothesGender != ClothesGender.UNISEX) {
                if (userGender == User.Gender.MALE && clothesGender != ClothesGender.MALE) continue;
                if (userGender == User.Gender.FEMALE && clothesGender != ClothesGender.FEMALE) continue;
            }
            scoredRecommendations.add(scoreCandidateWithUserStyles(wardrobe.getUser(), userStyles, clothes, currentTemp));
        }

        // [5] 정렬 및 동점자 처리
        // 1순위: 점수 내림차순
        // 2순위: 동점인 경우 랜덤 (Shuffle)
        Collections.shuffle(scoredRecommendations); // 먼저 섞음으로써 동점자 랜덤 효과
        scoredRecommendations.sort(Comparator.comparingDouble(ScoredRecommendation::score).reversed());

        // 결과 다양성 확보: 스타일 중복 최소화
        return pickDiverseResults(scoredRecommendations, MAX_RESULTS);
    }

    private List<RecommendResponse> pickDiverseResults(List<ScoredRecommendation> scoredRecommendations, int limit) {
        List<RecommendResponse> results = new ArrayList<>();
        Map<String, Integer> styleCounts = new HashMap<>();

        // 1차: 점수 순으로 보되, 특정 스타일이 과점하지 않도록 선택 (최대 40% 제한)
        int perStyleLimit = Math.max(2, (int) (limit * 0.4));

        for (ScoredRecommendation scored : scoredRecommendations) {
            if (results.size() >= limit) break;

            String style = scored.clothes().getRecommendationTagSnapshot().primaryStyleCode();
            if (style == null) style = "CASUAL";

            int count = styleCounts.getOrDefault(style, 0);
            if (count < perStyleLimit) {
                results.add(mapToRecommendResponse(scored));
                styleCounts.put(style, count + 1);
            }
        }

        // 2차: 부족한 개수만큼 다시 점수 순으로 채움
        if (results.size() < limit) {
            Set<Long> alreadyPicked = results.stream()
                    .map(RecommendResponse::clothesId)
                    .collect(Collectors.toSet());

            for (ScoredRecommendation scored : scoredRecommendations) {
                if (results.size() >= limit) break;
                if (!alreadyPicked.contains(scored.clothes().getId())) {
                    results.add(mapToRecommendResponse(scored));
                }
            }
        }

        return results;
    }

    private ScoredRecommendation scoreCandidateWithUserStyles(User user, List<UserStyle> userStyles, Clothes clothes, double currentTemp) {
        ClothesTagSnapshot snapshot = clothes.getRecommendationTagSnapshot();
        List<String> candidateStyles = snapshot.styleCodes();

        // 1. 스타일 점수 (60%)
        double maxStyleScore = 0.0;

        if (userStyles.isEmpty()) {
            // cold start — 성별 기반 기본값
            String primary = snapshot.primaryStyleCode();
            if (user.getGender() == User.Gender.FEMALE) {
                maxStyleScore = ("CHIC".equals(primary) || "CASUAL".equals(primary)) ? 0.8 : 0.0;
            } else {
                maxStyleScore = "CASUAL".equals(primary) ? 0.8 : 0.0;
            }
        } else {
            // 사용자의 모든 스타일 가중치 합산 (최대 1.0)
            for (UserStyle userStyle : userStyles) {
                String userStyleCode = userStyle.getStyle().getCode();
                // combinedWeight가 0~100 범위라고 가정 (WardrobeStatisticsService에서 100분율로 계산됨)
                double weight = Math.min(userStyle.getCombinedWeight(), 100) / 100.0;

                if (candidateStyles.contains(userStyleCode)) {
                    // 해당 옷이 사용자가 선호하는 스타일을 가지고 있으면 점수 부여
                    // PRIMARY 스타일이면 가중치 100%, SECONDARY면 60% 반영
                    double matchPower = userStyleCode.equals(snapshot.primaryStyleCode()) ? 1.0 : 0.6;
                    maxStyleScore = Math.max(maxStyleScore, weight * matchPower);
                }
            }
        }

        // 2. 날씨 점수 (40%)
        double weatherScore = WeatherCompatibilityTable.getWeatherScore(currentTemp, clothes.getItemType());
        // 3. 계절 일치 점수 (추가)
        ClothesSeason currentSeason = ClothesSeason.fromTemperature(currentTemp);
        double seasonMatchScore = currentSeason.isCompatibleWith(clothes.getSeason()) ? 1.0 : 0.0;

        // 최종 점수 계산 (스타일 비중 유지하되 날씨/계절 합산)
        double totalScore = (STYLE_WEIGHT * maxStyleScore) + (WEATHER_WEIGHT * weatherScore * 0.7) + (0.12 * seasonMatchScore);
        String reason = String.format("Style: %.1f, Weather: %.1f, Season: %.1f", maxStyleScore, weatherScore, seasonMatchScore);
        return new ScoredRecommendation(clothes, totalScore, reason);
    }

    private RecommendResponse mapToRecommendResponse(ScoredRecommendation scored) {
        Clothes clothes = scored.clothes();
        ClothesTagSnapshot snapshot = clothes.getRecommendationTagSnapshot();

        return new RecommendResponse(
                clothes.getName(),
                clothes.getExternalProductUrl(),
                clothes.getImageUrl(),
                String.format("%.2f", scored.score()),
                scored.reason(),
                clothes.getBrandName(),
                clothes.getCategory(),
                clothes.getItemType(),
                snapshot.primaryColor(),
                RecommendResponse.toColorDisplay(snapshot.primaryColor()),
                snapshot.primaryStyleCode(),
                clothes.getId()
        );
    }
    private record ScoredRecommendation(Clothes clothes, double score, String reason) {}
}
