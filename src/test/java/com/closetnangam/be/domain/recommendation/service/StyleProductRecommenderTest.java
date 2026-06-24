package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

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

    private boolean invokeIsEphemeralProductCode(String productCode) {
        return ReflectionTestUtils.invokeMethod(StyleProductRecommender.class, "isEphemeralProductCode", productCode);
    }
}
