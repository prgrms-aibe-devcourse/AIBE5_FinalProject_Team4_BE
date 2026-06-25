package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import com.closetnangam.be.global.external.naver.support.BrandGenderCorrector;
import com.closetnangam.be.global.external.naver.support.WidePantsGenderCorrector;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RECO-004 외부 후보 Gemini 분류.
 * 상품 이미지와 네이버 메타정보를 함께 보내 카탈로그 코드를 추론한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplementaryRecommendationClassificationService {

    private final GeminiService geminiService;
    private final CategoryCatalogService categoryCatalogService;
    private final StyleRepository styleRepository;
    private final RestTemplate restTemplate;

    @Value("${app.recommendation.complementary.ai-classification-enabled:true}")
    private boolean aiClassificationEnabled;

    public Optional<ResolvedClassification> resolveBatchClassification(GeminiClothingClassificationResult result) {
        try {
            validateClassificationResult(result);
            return Optional.of(toResolvedClassification(result));
        } catch (Exception exception) {
            log.warn("[RECO-004] Batch 분류 결과 검증 실패. reason={}", exception.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ResolvedClassification> classifyWithGemini(
            NaverShoppingProductResponse product,
            String cleanTitle
    ) {
        if (!aiClassificationEnabled) {
            return Optional.empty();
        }
        if (product == null || !StringUtils.hasText(product.image())) {
            return Optional.empty();
        }

        try {
            DownloadedImage image = downloadProductImage(product.image());
            GeminiClothingClassificationResult result = geminiService.classifyShoppingProduct(
                    image.bytes(),
                    image.mimeType(),
                    categoryCatalogService.getAiClassificationGuide(),
                    cleanTitle,
                    product.brand(),
                    product.category3()
            );
            validateClassificationResult(result);
            return Optional.of(toResolvedClassification(result, product.brand(), cleanTitle));
        } catch (Exception exception) {
            if (isContextRefreshFailure(exception)) {
                throw wrapAsUnchecked(exception);
            }
            log.warn(
                    "[RECO-004] Gemini 분류 실패 — 규칙 기반 fallback. productId={}, reason={}",
                    product.productId(),
                    exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private void validateClassificationResult(GeminiClothingClassificationResult result) {
        if (!StringUtils.hasText(result.category())
                || !StringUtils.hasText(result.itemType())
                || !StringUtils.hasText(result.primaryColor())
                || result.styles() == null
                || result.styles().isEmpty()) {
            throw new IllegalStateException("AI 판별 결과가 충분하지 않습니다.");
        }
        categoryCatalogService.validateCategoryAndItemType(result.category(), result.itemType());
        categoryCatalogService.validateClothesColors(
                result.primaryColor(),
                normalizeSecondaryColors(result.secondaryColors())
        );
        categoryCatalogService.validateStyleCodes(result.styles());
        categoryCatalogService.validateGenderCode(
                categoryCatalogService.resolveGenderOrDefault(result.gender()).name()
        );
        categoryCatalogService.validateSeasonCode(
                categoryCatalogService.resolveSeasonOrDefault(result.season()).name()
        );
    }

    private ResolvedClassification toResolvedClassification(GeminiClothingClassificationResult result) {
        return toResolvedClassification(result, result.brandName(), result.name());
    }

    private ResolvedClassification toResolvedClassification(
            GeminiClothingClassificationResult result,
            String brandName,
            String productTitle
    ) {
        CategoryCatalogService.ResolvedClothesColors resolvedColors = categoryCatalogService.resolveClothesColors(
                result.primaryColor(),
                normalizeSecondaryColors(result.secondaryColors())
        );

        List<ClothingColorDto> colors = new ArrayList<>();
        colors.add(new ClothingColorDto(resolvedColors.primaryColor(), ColorRole.PRIMARY, (byte) 0));

        byte secondaryOrder = 1;
        for (String secondaryColor : resolvedColors.secondaryColors()) {
            colors.add(new ClothingColorDto(secondaryColor, ColorRole.SECONDARY, secondaryOrder++));
        }

        Map<String, Style> styleMap = styleRepository.findByCodeIn(result.styles()).stream()
                .collect(Collectors.toMap(Style::getCode, Function.identity()));
        if (styleMap.size() != result.styles().size()) {
            throw new IllegalStateException("AI가 유효하지 않은 스타일 코드를 반환했습니다.");
        }

        List<ClothesStyleDto> styles = new ArrayList<>();
        byte styleOrder = 0;
        for (String styleCode : result.styles()) {
            Style style = styleMap.get(styleCode);
            StyleRole styleRole = styleOrder == 0 ? StyleRole.PRIMARY : StyleRole.SECONDARY;
            styles.add(new ClothesStyleDto(style.getId(), styleRole, styleOrder++));
        }

        return new ResolvedClassification(
                result.category(),
                result.itemType(),
                WidePantsGenderCorrector.correctGender(
                        BrandGenderCorrector.correctGender(
                                categoryCatalogService.resolveGenderOrDefault(result.gender()).name(),
                                brandName
                        ),
                        result.category(),
                        brandName,
                        productTitle
                ),
                categoryCatalogService.resolveSeasonOrDefault(result.season()).name(),
                colors,
                styles
        );
    }

    private List<String> normalizeSecondaryColors(List<String> secondaryColors) {
        return secondaryColors == null ? Collections.emptyList() : secondaryColors;
    }

    private DownloadedImage downloadProductImage(String imageUrl) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    byte[].class
            );
            byte[] imageBytes = response.getBody();
            if (imageBytes == null || imageBytes.length == 0) {
                throw new IllegalStateException("상품 이미지가 비어 있습니다.");
            }
            return new DownloadedImage(imageBytes, resolveMimeType(imageUrl, response.getHeaders()));
        } catch (RestClientException exception) {
            throw new IllegalStateException("상품 이미지를 다운로드하지 못했습니다.", exception);
        }
    }

    private String resolveMimeType(String imageUrl, HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType != null && "image".equals(contentType.getType())) {
            return contentType.toString();
        }

        String normalizedUrl = imageUrl.toLowerCase();
        if (normalizedUrl.contains(".png")) {
            return "image/png";
        }
        if (normalizedUrl.contains(".webp")) {
            return "image/webp";
        }
        if (normalizedUrl.contains(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    public record ResolvedClassification(
            String category,
            String itemType,
            String gender,
            String season,
            List<ClothingColorDto> colors,
            List<ClothesStyleDto> styles
    ) {
    }

    private record DownloadedImage(byte[] bytes, String mimeType) {
    }

    private static boolean isContextRefreshFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (
                    message.contains("Could not bind properties")
                            || message.contains("BeanFactory has been closed")
                            || message.contains("ApplicationContext has been closed")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static RuntimeException wrapAsUnchecked(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(exception);
    }
}
