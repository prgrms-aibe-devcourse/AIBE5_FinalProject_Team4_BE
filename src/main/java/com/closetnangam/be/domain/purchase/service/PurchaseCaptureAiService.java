package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureAnalyzeResponse;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.domain.purchase.repository.PurchaseCaptureRepository;
import com.closetnangam.be.domain.purchase.support.PurchaseCaptureDraftSupport;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import com.closetnangam.be.global.storage.StoredImageAnalysisContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class PurchaseCaptureAiService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseCaptureAiService.class);

    private final PurchaseCaptureRepository purchaseCaptureRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final GeminiService geminiService;
    private final LocalImageStorageService localImageStorageService;
    private final PurchaseCaptureThumbnailService purchaseCaptureThumbnailService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PurchaseCaptureAiService(
            PurchaseCaptureRepository purchaseCaptureRepository,
            CategoryCatalogService categoryCatalogService,
            GeminiService geminiService,
            LocalImageStorageService localImageStorageService,
            PurchaseCaptureThumbnailService purchaseCaptureThumbnailService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.purchaseCaptureRepository = purchaseCaptureRepository;
        this.categoryCatalogService = categoryCatalogService;
        this.geminiService = geminiService;
        this.localImageStorageService = localImageStorageService;
        this.purchaseCaptureThumbnailService = purchaseCaptureThumbnailService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PurchaseCaptureAnalyzeResponse analyzePurchaseCapture(Long userId, Long captureId) {
        StoredImageAnalysisContext ctx = transactionTemplate.execute(status -> {
            PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_IdForUpdate(captureId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
            if (capture.isAlreadySaved()) {
                throw new IllegalStateException("이미 저장된 구매내역 캡처는 다시 분석할 수 없습니다.");
            }
            capture.markAnalyzing();
            return new StoredImageAnalysisContext(capture.getStoredPath(), capture.getContentType());
        });
        if (ctx == null) {
            throw new IllegalStateException("분석 준비 중 오류가 발생했습니다.");
        }

        GeminiPurchaseCaptureExtractionResult result = null;
        String failureMessage = null;
        try {
            byte[] imageBytes = localImageStorageService.readStoredImage(ctx.storedPath());
            result = geminiService.extractPurchaseCaptureInfo(
                    imageBytes,
                    ctx.contentType(),
                    categoryCatalogService.getPurchaseCaptureExtractionGuide()
            );
            List<GeminiPurchaseCaptureItem> items = normalizeItems(
                    PurchaseCaptureDraftSupport.resolveItems(result)
            );
            PurchaseCaptureDraftSupport.validateExtractionItems(items, categoryCatalogService);
        } catch (IllegalArgumentException e) {
            log.warn("[구매내역AI] 이미지 파일을 읽지 못했습니다. userId={}, captureId={}: {}", userId, captureId, e.getMessage());
            failureMessage = "업로드된 이미지를 찾을 수 없습니다.";
        } catch (IllegalStateException e) {
            log.warn("[구매내역AI] AI 분석 실패. userId={}, captureId={}: {}", userId, captureId, e.getMessage());
            failureMessage = "구매내역 캡처를 분석하지 못했습니다. 직접 입력해 주세요.";
        } catch (Exception e) {
            log.error("[구매내역AI] 예상치 못한 오류. userId={}, captureId={}", userId, captureId, e);
            failureMessage = "AI 분석 중 예기치 않은 오류가 발생했습니다. 직접 입력해 주세요.";
        }

        final GeminiPurchaseCaptureExtractionResult finalResult = result;
        final String finalFailure = failureMessage;
        PurchaseCaptureAnalyzeResponse response = transactionTemplate.execute(status -> {
            PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_IdForUpdate(captureId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
            if (capture.isAlreadySaved()) {
                return PurchaseCaptureDraftSupport.toAnalyzeResponse(capture, objectMapper);
            }
            if (finalResult != null) {
                List<GeminiPurchaseCaptureItem> items = normalizeItems(
                        PurchaseCaptureDraftSupport.resolveItems(finalResult)
                );
                items = purchaseCaptureThumbnailService.enrichWithItemThumbnails(
                        userId,
                        capture.getId(),
                        capture.getImageUrl(),
                        capture.getStoredPath(),
                        items
                );
                GeminiPurchaseCaptureItem first = items.get(0);
                capture.applyAnalysisSuccess(
                        first.name(),
                        first.brandName(),
                        first.category(),
                        first.itemType(),
                        first.primaryColor(),
                        PurchaseCaptureDraftSupport.toColorsJson(first.secondaryColors(), objectMapper),
                        PurchaseCaptureDraftSupport.toStylesJson(first.styles(), objectMapper),
                        first.optionText(),
                        normalizeSuggestedExternalSource(first.suggestedExternalSource()),
                        PurchaseCaptureDraftSupport.toItemsJson(items, objectMapper),
                        toRawJson(finalResult)
                );
            } else {
                capture.applyAnalysisFailure(finalFailure, null);
            }
            return PurchaseCaptureDraftSupport.toAnalyzeResponse(capture, objectMapper);
        });
        if (response == null) {
            throw new IllegalStateException("분석 결과 저장 중 오류가 발생했습니다.");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PurchaseCaptureAnalyzeResponse getAnalyzeResult(Long userId, Long captureId) {
        PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_Id(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
        return PurchaseCaptureDraftSupport.toAnalyzeResponse(capture, objectMapper);
    }

    private List<GeminiPurchaseCaptureItem> normalizeItems(List<GeminiPurchaseCaptureItem> items) {
        return items.stream()
                .map(item -> new GeminiPurchaseCaptureItem(
                        item.name(),
                        item.brandName(),
                        item.category(),
                        item.itemType(),
                        item.primaryColor(),
                        item.secondaryColors(),
                        item.styles(),
                        item.optionText(),
                        normalizeSuggestedExternalSource(item.suggestedExternalSource()),
                        item.imageUrl(),
                        item.thumbnailRegion()
                ))
                .toList();
    }

    private String normalizeSuggestedExternalSource(String suggestedExternalSource) {
        if (!StringUtils.hasText(suggestedExternalSource)) {
            return null;
        }
        String normalized = suggestedExternalSource.trim().toUpperCase(Locale.ROOT);
        try {
            categoryCatalogService.validateExternalSource(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String toRawJson(GeminiPurchaseCaptureExtractionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            return result.toString();
        }
    }
}
