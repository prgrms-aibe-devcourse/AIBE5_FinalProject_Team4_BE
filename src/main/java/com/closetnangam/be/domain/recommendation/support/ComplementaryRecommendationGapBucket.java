package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.recommendation.support.ComplementaryRecommendationClassificationService.ResolvedClassification;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * RECO-004 후보 풀 부족 구간을 네이버 키워드로 보강할 때 사용하는 수집 버킷.
 * import 단계에서 Gemini 분류 결과가 버킷 조건과 맞을 때만 적재한다.
 */
public enum ComplementaryRecommendationGapBucket {

    OUTER_GENERAL(
            "outer-general",
            "아우터 전체",
            List.of(
                    "남성 패딩",
                    "여성 코트",
                    "남성 바람막이",
                    "여성 트렌치 코트",
                    "남성 블레이저",
                    "여성 가디건",
                    "경량 패딩",
                    "후드집업",
                    "남성 MA1",
                    "여성 숏패딩"
            ),
            classification -> "OUTER".equals(classification.category())
    ),
    OUTER_VARSITY_COACH(
            "outer-varsity-coach",
            "바시티·코치자켓",
            List.of(
                    "바시티 자켓",
                    "varsity jacket",
                    "코치자켓",
                    "코치 재킷",
                    "남성 바시티",
                    "여성 코치자켓"
            ),
            classification -> "OUTER".equals(classification.category())
                    && Set.of("VARSITY_JACKET", "COACH_JACKET").contains(classification.itemType())
    ),
    SHOES_HEEL_SPORT_SANDAL(
            "shoes-heel-sport-sandal",
            "힐·운동화·샌들",
            List.of(
                    "여성 힐",
                    "하이힐",
                    "운동화",
                    "러닝화",
                    "샌들",
                    "슬리퍼",
                    "여름 샌들",
                    "스포츠화"
            ),
            classification -> "SHOES".equals(classification.category())
                    && Set.of("HEEL", "SPORTS_SHOES", "SANDALS_SLIPPERS").contains(classification.itemType())
    ),
    TOP_HOOD_LONG_SLEEVELESS(
            "top-hood-long-sleeveless",
            "후드·롱슬리브·민소매",
            List.of(
                    "후드티",
                    "맨투맨 후드",
                    "롱슬리브 티셔츠",
                    "긴팔 티셔츠",
                    "민소매 티셔츠",
                    "나시",
                    "슬리브리스"
            ),
            classification -> "TOP".equals(classification.category())
                    && Set.of("HOODIE", "LONG_SLEEVE", "SLEEVELESS").contains(classification.itemType())
    ),
    MALE_UNISEX(
            "male-unisex",
            "남성·유니섹스",
            List.of(
                    "남성 티셔츠",
                    "남성 슬랙스",
                    "남성 데님",
                    "남성 스니커즈",
                    "유니섹스 맨투맨",
                    "남성 자켓",
                    "남성 후드",
                    "유니섹스 후드"
            ),
            classification -> "MALE".equals(classification.gender()) || "UNISEX".equals(classification.gender())
    );

    private static final Map<String, ComplementaryRecommendationGapBucket> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(bucket -> bucket.id, bucket -> bucket));

    private final String id;
    private final String label;
    private final List<String> keywords;
    private final Predicate<ResolvedClassification> acceptance;

    ComplementaryRecommendationGapBucket(
            String id,
            String label,
            List<String> keywords,
            Predicate<ResolvedClassification> acceptance
    ) {
        this.id = id;
        this.label = label;
        this.keywords = List.copyOf(keywords);
        this.acceptance = acceptance;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public List<String> keywords() {
        return keywords;
    }

    public boolean accepts(ResolvedClassification classification) {
        return classification != null && acceptance.test(classification);
    }

    public static List<ComplementaryRecommendationGapBucket> fillOrder() {
        return List.of(values());
    }

    public static Optional<ComplementaryRecommendationGapBucket> findById(String bucketId) {
        if (bucketId == null || bucketId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(bucketId.trim().toLowerCase(Locale.ROOT)));
    }
}
