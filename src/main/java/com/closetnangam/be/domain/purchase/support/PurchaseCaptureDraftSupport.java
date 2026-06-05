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
        // Gemini가 복수 행 화면에서 첫 상품만 flat으로 주는 경우가 있어, flat은 단일 후보만 복원합니다.
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
                result.thumbnailRegion(),
                result.orderStatus()
        ));
    }

    /**
     * 반품·환불·취소 등 옷장 등록 대상이 아닌 주문 행을 제거합니다.
     */
    public static List<GeminiPurchaseCaptureItem> filterRegistrableItems(List<GeminiPurchaseCaptureItem> items) {
        return items.stream()
                .filter(item -> !isExcludedOrderStatus(item.orderStatus()))
                .toList();
    }

    public static boolean isExcludedOrderStatus(String orderStatus) {
        if (!StringUtils.hasText(orderStatus)) {
            return false;
        }
        String normalized = orderStatus.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return normalized.contains("반품")
                || normalized.contains("환불")
                || normalized.contains("주문취소")
                || normalized.contains("취소완료")
                || normalized.startsWith("취소");
    }

    /**
     * 분석 단계 검증: 상품명만 필수. 카탈로그 코드는 사용자가 폼에서 수정할 수 있도록 느슨하게 둡니다.
     */
    public static void validateExtractionItems(List<GeminiPurchaseCaptureItem> items) {
        if (items.isEmpty()) {
            throw new IllegalStateException("등록 가능한 구매 상품이 없습니다. 반품·취소 내역만 있는 캡처인지 확인해 주세요.");
        }
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            if (!StringUtils.hasText(item.name())) {
                throw new IllegalStateException("AI 추출 결과 " + (index + 1) + "번째 상품에 상품명이 없습니다.");
            }
        }
    }

    public static List<GeminiPurchaseCaptureItem> normalizeExtractionItems(List<GeminiPurchaseCaptureItem> items) {
        List<GeminiPurchaseCaptureItem> normalized = new ArrayList<>(items.size());
        for (GeminiPurchaseCaptureItem item : items) {
            List<String> styles = item.styles();
            if (styles == null || styles.isEmpty()) {
                styles = List.of("CASUAL");
            }
            normalized.add(new GeminiPurchaseCaptureItem(
                    item.name(),
                    StringUtils.hasText(item.brandName()) ? item.brandName() : "UNKNOWN",
                    item.category(),
                    item.itemType(),
                    item.primaryColor(),
                    normalizeSecondaryColors(item.secondaryColors()),
                    styles,
                    item.optionText(),
                    item.suggestedExternalSource(),
                    item.imageUrl(),
                    item.thumbnailRegion(),
                    item.orderStatus()
            ));
        }
        return normalized;
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

        if (!storedItems.isEmpty()) {
            List<PurchaseCaptureItemDraft> itemDrafts = new ArrayList<>();
            for (int index = 0; index < storedItems.size(); index++) {
                itemDrafts.add(toItemDraft(index, storedItems.get(index), capture.getImageUrl()));
            }
            if (storedItems.size() > 1) {
                return new DraftView(null, null, null, null, null, null, null, null, null, itemDrafts);
            }
            GeminiPurchaseCaptureItem first = storedItems.get(0);
            return new DraftView(
                    first.name(),
                    first.brandName(),
                    first.category(),
                    first.itemType(),
                    first.primaryColor(),
                    first.secondaryColors(),
                    first.styles(),
                    first.optionText(),
                    normalizeSuggestedExternalSource(first.suggestedExternalSource()),
                    itemDrafts
            );
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
