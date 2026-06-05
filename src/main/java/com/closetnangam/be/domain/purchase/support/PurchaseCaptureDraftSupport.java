package com.closetnangam.be.domain.purchase.support;

import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureAnalyzeResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureDraftResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureItemDraft;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PurchaseCaptureDraftSupport {

    private PurchaseCaptureDraftSupport() {
    }

    public static List<GeminiPurchaseCaptureItem> resolveItems(GeminiPurchaseCaptureExtractionResult result) {
        if (result.items() != null && !result.items().isEmpty()) {
            return result.items();
        }
        if (!StringUtils.hasText(result.name())) {
            return List.of();
        }
        return List.of(new GeminiPurchaseCaptureItem(
                result.name(),
                result.brandName(),
                result.category(),
                result.itemType(),
                result.primaryColor(),
                result.secondaryColors(),
                result.styles(),
                result.optionText(),
                result.suggestedExternalSource(),
                result.imageUrl(),
                result.thumbnailRegion()
        ));
    }

    public static void validateExtractionItems(
            List<GeminiPurchaseCaptureItem> items,
            CategoryCatalogService categoryCatalogService
    ) {
        if (items.isEmpty()) {
            throw new IllegalStateException("AI 추출 결과에 상품이 없습니다.");
        }
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            if (!StringUtils.hasText(item.name())) {
                throw new IllegalStateException("AI 추출 결과 " + (index + 1) + "번째 상품에 상품명이 없습니다.");
            }
            if (StringUtils.hasText(item.category()) && StringUtils.hasText(item.itemType())) {
                categoryCatalogService.validateCategoryAndItemType(item.category(), item.itemType());
            }
            if (StringUtils.hasText(item.primaryColor())) {
                categoryCatalogService.validateClothesColors(
                        item.primaryColor(),
                        normalizeSecondaryColors(item.secondaryColors())
                );
            }
            if (item.styles() != null && !item.styles().isEmpty()) {
                categoryCatalogService.validateStyleCodes(item.styles());
            }
            String normalizedExternalSource = normalizeExternalSourceCode(item.suggestedExternalSource());
            if (normalizedExternalSource != null) {
                categoryCatalogService.validateExternalSource(normalizedExternalSource);
            }
        }
    }

    public static String toItemsJson(List<GeminiPurchaseCaptureItem> items, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 추출 결과를 저장하지 못했습니다.");
        }
    }

    public static List<GeminiPurchaseCaptureItem> parseItems(String draftItemsJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(draftItemsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    draftItemsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GeminiPurchaseCaptureItem.class)
            );
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    public static int resolveItemIndex(Integer requestedIndex, int itemCount) {
        int index = requestedIndex != null ? requestedIndex : 0;
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException("유효하지 않은 itemIndex입니다.");
        }
        return index;
    }

    public static PurchaseCaptureAnalyzeResponse toAnalyzeResponse(PurchaseCapture capture, ObjectMapper objectMapper) {
        DraftView view = buildDraftView(capture, objectMapper);
        return new PurchaseCaptureAnalyzeResponse(
                capture.getId(),
                capture.getAnalysisStatus(),
                capture.getImageUrl(),
                capture.getFailureMessage(),
                capture.getAnalysisStatus() == com.closetnangam.be.domain.ai.enums.AiAnalysisStatus.FAILED,
                view.flatName(),
                view.flatBrandName(),
                view.flatCategory(),
                view.flatItemType(),
                view.flatPrimaryColor(),
                view.flatSecondaryColors(),
                view.flatStyles(),
                view.flatOptionText(),
                view.flatSuggestedExternalSource(),
                view.items()
        );
    }

    public static PurchaseCaptureDraftResponse toDraftResponse(PurchaseCapture capture, ObjectMapper objectMapper) {
        DraftView view = buildDraftView(capture, objectMapper);
        return new PurchaseCaptureDraftResponse(
                capture.getId(),
                capture.getAnalysisStatus(),
                capture.getImageUrl(),
                capture.getFailureMessage(),
                capture.getAnalysisStatus() == com.closetnangam.be.domain.ai.enums.AiAnalysisStatus.FAILED,
                view.flatName(),
                view.flatBrandName(),
                view.flatCategory(),
                view.flatItemType(),
                view.flatPrimaryColor(),
                view.flatSecondaryColors(),
                view.flatStyles(),
                view.flatOptionText(),
                view.flatSuggestedExternalSource(),
                view.items()
        );
    }

    private static DraftView buildDraftView(PurchaseCapture capture, ObjectMapper objectMapper) {
        List<GeminiPurchaseCaptureItem> storedItems = parseItems(capture.getDraftItemsJson(), objectMapper);
        boolean multiItem = storedItems.size() > 1;

        if (multiItem) {
            List<PurchaseCaptureItemDraft> itemDrafts = new ArrayList<>();
            for (int index = 0; index < storedItems.size(); index++) {
                itemDrafts.add(toItemDraft(index, storedItems.get(index), capture.getImageUrl()));
            }
            return new DraftView(null, null, null, null, null, null, null, null, null, itemDrafts);
        }

        return new DraftView(
                capture.getDraftName(),
                capture.getDraftBrandName(),
                capture.getDraftCategory(),
                capture.getDraftItemType(),
                capture.getDraftPrimaryColor(),
                parseStringList(capture.getDraftSecondaryColorsJson(), objectMapper),
                parseStringList(capture.getDraftStylesJson(), objectMapper),
                capture.getDraftOptionText(),
                capture.getDraftExternalSource(),
                null
        );
    }

    private static PurchaseCaptureItemDraft toItemDraft(
            int itemIndex,
            GeminiPurchaseCaptureItem item,
            String captureImageUrl
    ) {
        return new PurchaseCaptureItemDraft(
                itemIndex,
                item.name(),
                item.brandName(),
                item.category(),
                item.itemType(),
                item.primaryColor(),
                normalizeSecondaryColors(item.secondaryColors()),
                item.styles() != null ? item.styles() : List.of(),
                item.optionText(),
                normalizeSuggestedExternalSource(item.suggestedExternalSource()),
                resolveItemImageUrl(item.imageUrl(), captureImageUrl)
        );
    }

    /**
     * 상품 전용 썸네일만 내려줍니다. AI가 추출하지 못했거나 캡처 원본 URL과 같으면 null입니다.
     */
    static String resolveItemImageUrl(String extractedImageUrl, String captureImageUrl) {
        if (!StringUtils.hasText(extractedImageUrl)) {
            return null;
        }
        String normalizedExtracted = extractedImageUrl.trim();
        if (!StringUtils.hasText(captureImageUrl)) {
            return normalizedExtracted;
        }
        if (normalizedExtracted.equals(captureImageUrl.trim())) {
            return null;
        }
        return normalizedExtracted;
    }

    public static String toColorsJson(List<String> secondaryColors, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(normalizeSecondaryColors(secondaryColors));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 추출 결과를 저장하지 못했습니다.");
        }
    }

    public static String toStylesJson(List<String> styles, ObjectMapper objectMapper) {
        if (styles == null || styles.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(styles);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 추출 결과를 저장하지 못했습니다.");
        }
    }

    public static String normalizeSuggestedExternalSource(String suggestedExternalSource) {
        String normalized = normalizeExternalSourceCode(suggestedExternalSource);
        return normalized;
    }

    private static List<String> parseStringList(String json, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private static List<String> normalizeSecondaryColors(List<String> secondaryColors) {
        return secondaryColors == null ? Collections.emptyList() : secondaryColors;
    }

    private static String normalizeExternalSourceCode(String suggestedExternalSource) {
        if (!StringUtils.hasText(suggestedExternalSource)) {
            return null;
        }
        return suggestedExternalSource.trim().toUpperCase(Locale.ROOT);
    }

    private record DraftView(
            String flatName,
            String flatBrandName,
            String flatCategory,
            String flatItemType,
            String flatPrimaryColor,
            List<String> flatSecondaryColors,
            List<String> flatStyles,
            String flatOptionText,
            String flatSuggestedExternalSource,
            List<PurchaseCaptureItemDraft> items
    ) {
    }
}
