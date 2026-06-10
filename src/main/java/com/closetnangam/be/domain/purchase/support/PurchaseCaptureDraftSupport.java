package com.closetnangam.be.domain.purchase.support;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureAnalyzeResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureDraftResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureItemDraft;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.domain.purchase.enums.PurchaseCaptureItemStatus;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PurchaseCaptureDraftSupport {

    private static final TypeReference<Map<String, ItemProgressEntry>> PROGRESS_TYPE =
            new TypeReference<>() {
            };

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
                result.gender(),
                result.season(),
                result.optionText(),
                result.suggestedExternalSource(),
                result.imageUrl(),
                result.thumbnailRegion(),
                result.orderStatus()
        ));
    }

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

    /**
     * AI 추출 상품별 카탈로그 code를 검증합니다. 유효하지 않으면 분석 실패로 처리합니다.
     */
    public static void validateExtractionItemCatalogCodes(
            List<GeminiPurchaseCaptureItem> items,
            CategoryCatalogService categoryCatalogService
    ) {
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            int itemNumber = index + 1;
            if (!StringUtils.hasText(item.category())
                    || !StringUtils.hasText(item.itemType())
                    || !StringUtils.hasText(item.primaryColor())
                    || item.styles() == null
                    || item.styles().isEmpty()) {
                throw new IllegalStateException(
                        "AI 추출 결과 " + itemNumber + "번째 상품에 필수 분류 정보가 없습니다."
                );
            }
            try {
                categoryCatalogService.validateCategoryAndItemType(item.category(), item.itemType());
                categoryCatalogService.validateClothesColors(
                        item.primaryColor(),
                        normalizeSecondaryColors(item.secondaryColors())
                );
                categoryCatalogService.validateStyleCodes(item.styles());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                        "AI 추출 결과 " + itemNumber + "번째 상품의 분류 코드가 유효하지 않습니다: "
                                + exception.getMessage(),
                        exception
                );
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
                    resolveGenderOrDefault(item.gender()),
                    resolveSeasonOrDefault(item.season()),
                    item.optionText(),
                    item.suggestedExternalSource(),
                    item.imageUrl(),
                    item.thumbnailRegion(),
                    item.orderStatus()
            ));
        }
        return normalized;
    }

    /**
     * 사진·구매내역 등록 draft에는 사용자 프로필 성별을 기본 gender로 사용합니다.
     */
    public static List<GeminiPurchaseCaptureItem> applyRegistrationDefaultGender(
            List<GeminiPurchaseCaptureItem> items,
            String defaultGenderCode
    ) {
        return items.stream()
                .map(item -> new GeminiPurchaseCaptureItem(
                        item.name(),
                        item.brandName(),
                        item.category(),
                        item.itemType(),
                        item.primaryColor(),
                        item.secondaryColors(),
                        item.styles(),
                        defaultGenderCode,
                        resolveSeasonOrDefault(item.season()),
                        item.optionText(),
                        item.suggestedExternalSource(),
                        item.imageUrl(),
                        item.thumbnailRegion(),
                        item.orderStatus()
                ))
                .toList();
    }

    public static String registrationDefaultGenderCode(PurchaseCapture capture) {
        return ClothesGender.fromUserGender(capture.getUser().getGender()).name();
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

    public static List<GeminiPurchaseCaptureItem> parseRegistrableItems(String draftItemsJson, ObjectMapper objectMapper) {
        return filterRegistrableItems(parseItems(draftItemsJson, objectMapper));
    }

    public static int countRegistrableItems(List<GeminiPurchaseCaptureItem> items) {
        return (int) items.stream()
                .filter(item -> !isExcludedOrderStatus(item.orderStatus()))
                .count();
    }

    public static Map<String, ItemProgressEntry> parseProgress(String itemProgressJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(itemProgressJson)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, ItemProgressEntry> parsed = objectMapper.readValue(itemProgressJson, PROGRESS_TYPE);
            return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
        } catch (JsonProcessingException exception) {
            return new LinkedHashMap<>();
        }
    }

    public static String toProgressJson(Map<String, ItemProgressEntry> progress, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(progress);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("상품 처리 상태를 저장하지 못했습니다.");
        }
    }

    public static int resolveItemIndex(Integer requestedIndex, int itemCount) {
        if (itemCount <= 0) {
            throw new IllegalArgumentException("저장할 구매내역 상품이 없습니다.");
        }
        int index = requestedIndex != null ? requestedIndex : 0;
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException("유효하지 않은 itemIndex입니다. itemIndex=" + index);
        }
        return index;
    }

    public static PurchaseCaptureItemStatus resolveItemStatus(
            Map<String, ItemProgressEntry> progress,
            int itemIndex
    ) {
        ItemProgressEntry entry = progress.get(String.valueOf(itemIndex));
        if (entry == null || entry.status() == null) {
            return PurchaseCaptureItemStatus.PENDING;
        }
        return entry.status();
    }

    public static boolean hasAnyProcessedItem(Map<String, ItemProgressEntry> progress) {
        return progress.values().stream()
                .anyMatch(entry -> entry.status() == PurchaseCaptureItemStatus.SAVED
                        || entry.status() == PurchaseCaptureItemStatus.SKIPPED);
    }

    public static boolean isAllItemsProcessed(Map<String, ItemProgressEntry> progress, int itemCount) {
        if (itemCount <= 0) {
            return false;
        }
        for (int index = 0; index < itemCount; index++) {
            if (resolveItemStatus(progress, index) == PurchaseCaptureItemStatus.PENDING) {
                return false;
            }
        }
        return true;
    }

    public static int countPendingItems(Map<String, ItemProgressEntry> progress, int itemCount) {
        int pending = 0;
        for (int index = 0; index < itemCount; index++) {
            if (resolveItemStatus(progress, index) == PurchaseCaptureItemStatus.PENDING) {
                pending++;
            }
        }
        return pending;
    }

    /**
     * draft/analyze 응답용. 등록 가능 상품이 1개뿐이면 캡처 URL fallback을 허용합니다.
     */
    public static String resolveItemPreviewImageUrl(
            String extractedImageUrl,
            String captureImageUrl,
            int registrableItemCount
    ) {
        String distinct = resolveDistinctItemImageUrl(extractedImageUrl, captureImageUrl);
        if (StringUtils.hasText(distinct)) {
            return distinct;
        }
        if (registrableItemCount <= 1 && StringUtils.hasText(captureImageUrl)) {
            return captureImageUrl.trim();
        }
        return null;
    }

    public static String resolveClothesImageUrl(
            String captureImageUrl,
            List<GeminiPurchaseCaptureItem> items,
            int itemIndex,
            String requestImageUrl
    ) {
        if (StringUtils.hasText(requestImageUrl)) {
            return requestImageUrl;
        }
        if (itemIndex >= 0 && itemIndex < items.size()) {
            String itemImageUrl = resolveDistinctItemImageUrl(items.get(itemIndex).imageUrl(), captureImageUrl);
            if (StringUtils.hasText(itemImageUrl)) {
                return itemImageUrl;
            }
        }
        if (StringUtils.hasText(captureImageUrl)) {
            return captureImageUrl.trim();
        }
        throw new IllegalArgumentException(
                "구매내역 캡처 이미지 URL이 없습니다. itemIndex=" + itemIndex
        );
    }

    static String resolveDistinctItemImageUrl(String extractedImageUrl, String captureImageUrl) {
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

    public static PurchaseCaptureAnalyzeResponse toAnalyzeResponse(PurchaseCapture capture, ObjectMapper objectMapper) {
        PurchaseCaptureDraftResponse draft = toDraftResponse(capture, objectMapper);
        return new PurchaseCaptureAnalyzeResponse(
                draft.captureId(),
                draft.analysisStatus(),
                draft.previewUrl(),
                draft.failureMessage(),
                draft.aiFailed(),
                draft.name(),
                draft.brandName(),
                draft.category(),
                draft.itemType(),
                draft.gender(),
                draft.season(),
                draft.primaryColor(),
                draft.secondaryColors(),
                draft.styles(),
                draft.optionText(),
                draft.suggestedExternalSource(),
                draft.items(),
                draft.pendingItemCount(),
                draft.captureCompleted()
        );
    }

    public static PurchaseCaptureDraftResponse toDraftResponse(PurchaseCapture capture, ObjectMapper objectMapper) {
        DraftView view = buildDraftView(capture, objectMapper);
        List<GeminiPurchaseCaptureItem> storedItems = parseRegistrableItems(capture.getDraftItemsJson(), objectMapper);
        Map<String, ItemProgressEntry> progress = parseProgress(capture.getItemProgressJson(), objectMapper);
        return new PurchaseCaptureDraftResponse(
                capture.getId(),
                capture.getAnalysisStatus(),
                capture.getImageUrl(),
                capture.getFailureMessage(),
                capture.getAnalysisStatus() == AiAnalysisStatus.FAILED,
                view.flatName(),
                view.flatBrandName(),
                view.flatCategory(),
                view.flatItemType(),
                view.flatGender(),
                view.flatSeason(),
                view.flatPrimaryColor(),
                view.flatSecondaryColors(),
                view.flatStyles(),
                view.flatOptionText(),
                view.flatSuggestedExternalSource(),
                view.items(),
                countPendingItems(progress, storedItems.size()),
                capture.isFullyProcessed()
        );
    }

    private static DraftView buildDraftView(PurchaseCapture capture, ObjectMapper objectMapper) {
        List<GeminiPurchaseCaptureItem> storedItems = parseRegistrableItems(capture.getDraftItemsJson(), objectMapper);
        Map<String, ItemProgressEntry> progress = parseProgress(capture.getItemProgressJson(), objectMapper);
        int registrableItemCount = storedItems.size();
        String defaultGenderCode = registrationDefaultGenderCode(capture);

        if (!storedItems.isEmpty()) {
            List<PurchaseCaptureItemDraft> itemDrafts = new ArrayList<>();
            for (int index = 0; index < storedItems.size(); index++) {
                itemDrafts.add(toItemDraft(
                        index,
                        storedItems.get(index),
                        capture.getImageUrl(),
                        registrableItemCount,
                        progress,
                        defaultGenderCode
                ));
            }
            if (storedItems.size() > 1) {
                return new DraftView(null, null, null, null, null, null, null, null, null, null, null, itemDrafts);
            }
            GeminiPurchaseCaptureItem first = storedItems.get(0);
            return new DraftView(
                    first.name(),
                    first.brandName(),
                    first.category(),
                    first.itemType(),
                    defaultGenderCode,
                    resolveSeasonOrDefault(first.season()),
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
                defaultGenderCode,
                "ALL_SEASON",
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
            String captureImageUrl,
            int registrableItemCount,
            Map<String, ItemProgressEntry> progress,
            String defaultGenderCode
    ) {
        return new PurchaseCaptureItemDraft(
                itemIndex,
                resolveItemStatus(progress, itemIndex),
                item.name(),
                item.brandName(),
                item.category(),
                item.itemType(),
                item.primaryColor(),
                normalizeSecondaryColors(item.secondaryColors()),
                item.styles() != null ? item.styles() : List.of(),
                defaultGenderCode,
                resolveSeasonOrDefault(item.season()),
                item.optionText(),
                normalizeSuggestedExternalSource(item.suggestedExternalSource()),
                resolveItemPreviewImageUrl(item.imageUrl(), captureImageUrl, registrableItemCount)
        );
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
        return normalizeExternalSourceCode(suggestedExternalSource);
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

    private static String resolveGenderOrDefault(String gender) {
        return StringUtils.hasText(gender) ? gender : "UNISEX";
    }

    private static String resolveSeasonOrDefault(String season) {
        return ClothesSeason.fromCodeOrDefault(season).name();
    }

    public record ItemProgressEntry(
            PurchaseCaptureItemStatus status,
            Long clothesId,
            Long wardrobeClothesId
    ) {
    }

    private record DraftView(
            String flatName,
            String flatBrandName,
            String flatCategory,
            String flatItemType,
            String flatGender,
            String flatSeason,
            String flatPrimaryColor,
            List<String> flatSecondaryColors,
            List<String> flatStyles,
            String flatOptionText,
            String flatSuggestedExternalSource,
            List<PurchaseCaptureItemDraft> items
    ) {
    }
}
