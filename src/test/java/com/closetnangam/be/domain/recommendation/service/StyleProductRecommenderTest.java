package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.scoring.ClothesTagSnapshot;
import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.domain.recommendation.repository.RecommendationFeedbackRepository;
import com.closetnangam.be.domain.user.repository.UserStyleRepository;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import com.closetnangam.be.domain.wardrobe.service.WardrobeStatisticsService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StyleProductRecommenderTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private WardrobeClothesRepository wardrobeClothesRepository;

    @Mock
    private WardrobeRepository wardrobeRepository;

    @Mock
    private UserStyleRepository userStyleRepository;

    @Mock
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @Mock
    private WardrobeStatisticsService wardrobeStatisticsService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StyleProductRecommender styleProductRecommender;

    @Test
    @DisplayName("isEphemeralProductCode 메서드는 임시 코드 형식을 올바르게 판별한다")
    void isEphemeralProductCodeTest() {
        // "PHOTO-abc123" → true (임시 코드)
        assertThat(invokeIsEphemeralProductCode("PHOTO-abc123")).isTrue();
        // "PURCHASE-xyz" → true (임시 코드)
        assertThat(invokeIsEphemeralProductCode("PURCHASE-xyz")).isTrue();
        // "UNKNOWN" → true (임시 코드)
        assertThat(invokeIsEphemeralProductCode("UNKNOWN")).isTrue();
        // null → true (null은 임시로 취급)
        assertThat(invokeIsEphemeralProductCode(null)).isTrue();
        // "" → true (빈 문자열은 임시로 취급)
        assertThat(invokeIsEphemeralProductCode("")).isTrue();
        // "NV12345678" → false (정상 코드)
        assertThat(invokeIsEphemeralProductCode("NV12345678")).isFalse();
    }

    @Test
    @DisplayName("ClothesRepository.findAllForRecommendation() 쿼리는 EXTERNAL_SHOPPING만 조회하고 PURCHASE_HISTORY는 제외한다")
    void findAllForRecommendationQueryVerification() throws NoSuchMethodException {
        // ClothesRepository.findAllForRecommendation()이
        // EXTERNAL_SHOPPING 옷만 반환하고
        // PURCHASE_HISTORY 옷은 반환하지 않는지 검증
        // @Query 어노테이션의 쿼리 조건을 확인하는 방식으로 작성

        Method method = ClothesRepository.class.getMethod("findAllForRecommendation", Pageable.class);
        Query queryAnnotation = method.getAnnotation(Query.class);

        assertThat(queryAnnotation).isNotNull();
        String queryValue = queryAnnotation.value();

        // EXTERNAL_SHOPPING 포함 여부 확인
        assertThat(queryValue).contains("c.clothesInfoSource = 'EXTERNAL_SHOPPING'");
        // PURCHASE_HISTORY 제외 여부 확인 (쿼리에 포함되지 않아야 함)
        assertThat(queryValue).doesNotContain("PURCHASE_HISTORY");
    }

    @Test
    @DisplayName("pickDiverseResults는 카테고리당 최대 30% 제한을 적용하고 부족하면 제한을 풀어 채운다")
    void pickDiverseResultsCategoryLimitTest() {
        // limit = 10 이면 perCategoryLimit = max(2, 3) = 3
        int limit = 10;
        List<Object> scoredRecommendations = new ArrayList<>();

        // 1. "SHOES" 카테고리 5개 (점수 90~86)
        for (int i = 0; i < 5; i++) {
            scoredRecommendations.add(createMockScoredRecommendation((long) i, "SHOES", "CASUAL", 90.0 - i));
        }
        // 2. "TOP" 카테고리 2개 (점수 80~79)
        for (int i = 5; i < 7; i++) {
            scoredRecommendations.add(createMockScoredRecommendation((long) i, "TOP", "CASUAL", 80.0 - i));
        }
        // 3. "BOTTOM" 카테고리 10개 (점수 70~61)
        for (int i = 7; i < 17; i++) {
            scoredRecommendations.add(createMockScoredRecommendation((long) i, "BOTTOM", "CASUAL", 70.0 - i));
        }

        // private 메서드 호출
        List<RecommendResponse> results = ReflectionTestUtils.invokeMethod(styleProductRecommender, "pickDiverseResults", scoredRecommendations, limit);

        assertThat(results).hasSize(limit);

        // 카테고리별 개수 집계
        Map<String, Long> categoryCounts = results.stream()
                .collect(Collectors.groupingBy(RecommendResponse::category, Collectors.counting()));

        // 1차 선별에서 SHOES는 3개만 뽑혔어야 함.
        // TOP 2개 모두 뽑힘.
        // BOTTOM 1차에서 3개 뽑힘.
        // 현재까지 3(SHOES) + 2(TOP) + 3(BOTTOM) = 8개.
        // 남은 2개는 2차 보충에서 점수 순으로 채워짐.
        // SHOES(87.0), SHOES(86.0) 가 점수가 높으므로 2차에서 채워질 것.
        // 최종 예상: SHOES 5개, TOP 2개, BOTTOM 3개
        assertThat(categoryCounts.get("SHOES")).isEqualTo(5L);
        assertThat(categoryCounts.get("TOP")).isEqualTo(2L);
        assertThat(categoryCounts.get("BOTTOM")).isEqualTo(3L);
    }

    private Object createMockScoredRecommendation(Long id, String category, String style, double score) {
        Clothes clothes = mock(Clothes.class);
        lenient().when(clothes.getId()).thenReturn(id);
        lenient().when(clothes.getCategory()).thenReturn(category);
        
        ClothesTagSnapshot snapshot = mock(ClothesTagSnapshot.class);
        lenient().when(snapshot.primaryStyleCode()).thenReturn(style);
        lenient().when(clothes.getRecommendationTagSnapshot()).thenReturn(snapshot);

        // ScoredRecommendation record 인스턴스 생성 (Reflection 이용)
        try {
            Class<?> srClass = Class.forName("com.closetnangam.be.domain.recommendation.service.StyleProductRecommender$ScoredRecommendation");
            java.lang.reflect.Constructor<?> constructor = srClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return constructor.newInstance(clothes, score, score, "Reason");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean invokeIsEphemeralProductCode(String productCode) {
        return ReflectionTestUtils.invokeMethod(StyleProductRecommender.class, "isEphemeralProductCode", productCode);
    }
}
