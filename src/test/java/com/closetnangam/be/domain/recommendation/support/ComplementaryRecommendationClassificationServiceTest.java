package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComplementaryRecommendationClassificationServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private CategoryCatalogService categoryCatalogService;

    @Mock
    private StyleRepository styleRepository;

    @Mock
    private RestTemplate restTemplate;

    private ComplementaryRecommendationClassificationService classificationService;

    @BeforeEach
    void setUp() {
        classificationService = new ComplementaryRecommendationClassificationService(
                geminiService,
                categoryCatalogService,
                styleRepository,
                restTemplate
        );
        ReflectionTestUtils.setField(classificationService, "aiClassificationEnabled", true);
    }

    @Test
    @DisplayName("Gemini 분류 성공 시 category/color/style 태그를 반환한다")
    void classifyWithGeminiReturnsResolvedClassification() {
        NaverShoppingProductResponse product = new NaverShoppingProductResponse(
                "<b>남성 블랙 데님</b>",
                "https://smartstore.naver.com/example",
                "https://shopping-phinf.pstatic.net/example.jpg",
                59000,
                59000,
                "스마트스토어",
                "p-1",
                "2",
                "테스트 브랜드",
                "테스트 브랜드",
                "패션",
                "남성",
                "청바지",
                null
        );

        given(restTemplate.exchange(
                eq(product.image()),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(byte[].class)
        )).willReturn(ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new byte[] {(byte) 0xFF, (byte) 0xD8, 0x01}));
        given(categoryCatalogService.getAiClassificationGuide()).willReturn("guide");
        given(geminiService.classifyShoppingProduct(
                any(),
                eq("image/jpeg"),
                eq("guide"),
                eq("남성 블랙 데님"),
                eq("테스트 브랜드"),
                eq("청바지")
        )).willReturn(new GeminiClothingClassificationResult(
                true,
                "남성 블랙 데님",
                "테스트 브랜드",
                "BOTTOM",
                "DENIM",
                "BLACK",
                List.of(),
                List.of("CASUAL", "STREET"),
                "MALE",
                "ALL_SEASON"
        ));
        given(categoryCatalogService.resolveGenderOrDefault("MALE")).willReturn(ClothesGender.MALE);
        given(categoryCatalogService.resolveSeasonOrDefault("ALL_SEASON")).willReturn(ClothesSeason.ALL_SEASON);

        Style casual = Style.from(StyleCode.CASUAL);
        ReflectionTestUtils.setField(casual, "id", 1L);
        Style street = Style.from(StyleCode.STREET);
        ReflectionTestUtils.setField(street, "id", 2L);
        given(styleRepository.findByCodeIn(List.of("CASUAL", "STREET"))).willReturn(List.of(casual, street));

        Optional<ComplementaryRecommendationClassificationService.ResolvedClassification> result =
                classificationService.classifyWithGemini(product, "남성 블랙 데님");

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo("BOTTOM");
        assertThat(result.get().itemType()).isEqualTo("DENIM");
        assertThat(result.get().gender()).isEqualTo("MALE");
        assertThat(result.get().season()).isEqualTo("ALL_SEASON");
        assertThat(result.get().colors()).hasSize(1);
        assertThat(result.get().styles()).hasSize(2);
        verify(categoryCatalogService).validateCategoryAndItemType("BOTTOM", "DENIM");
        verify(categoryCatalogService).validateClothesColors("BLACK", List.of());
        verify(categoryCatalogService).validateStyleCodes(List.of("CASUAL", "STREET"));
        verify(categoryCatalogService).validateGenderCode("MALE");
        verify(categoryCatalogService).validateSeasonCode("ALL_SEASON");
    }

    @Test
    @DisplayName("AI 분류가 비활성화되면 Gemini를 호출하지 않는다")
    void classifyWithGeminiSkipsWhenDisabled() {
        ReflectionTestUtils.setField(classificationService, "aiClassificationEnabled", false);

        NaverShoppingProductResponse product = new NaverShoppingProductResponse(
                "화이트 티셔츠",
                "https://smartstore.naver.com/example2",
                "https://shopping-phinf.pstatic.net/example2.jpg",
                19000,
                19000,
                "스마트스토어",
                "p-2",
                "2",
                "브랜드",
                "브랜드",
                "패션",
                "남성",
                "티셔츠",
                null
        );

        Optional<ComplementaryRecommendationClassificationService.ResolvedClassification> result =
                classificationService.classifyWithGemini(product, "화이트 티셔츠");

        assertThat(result).isEmpty();
        verify(geminiService, never()).classifyShoppingProduct(any(), any(), any(), any(), any(), any());
    }
}
