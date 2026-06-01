package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureAnalyzeResponse;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.domain.purchase.repository.PurchaseCaptureRepository;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class PurchaseCaptureAiService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseCaptureAiService.class);

    private final PurchaseCaptureRepository purchaseCaptureRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final GeminiService geminiService;
    private final LocalImageStorageService localImageStorageService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PurchaseCaptureAiService(
            PurchaseCaptureRepository purchaseCaptureRepository,
            CategoryCatalogService categoryCatalogService,
            GeminiService geminiService,
            LocalImageStorageService localImageStorageService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.purchaseCaptureRepository = purchaseCaptureRepository;
        this.categoryCatalogService = categoryCatalogService;
        this.geminiService = geminiService;
        this.localImageStorageService = localImageStorageService;
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
            validateExtractionResult(result);
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
                return toAnalyzeResponse(capture);
            }
            if (finalResult != null) {
                capture.applyAnalysisSuccess(
                        finalResult.name(),
                        finalResult.brandName(),
                        finalResult.category(),
                        finalResult.itemType(),
                        finalResult.primaryColor(),
                        toColorsJson(finalResult.secondaryColors()),
                        toStylesJson(finalResult.styles()),
                        finalResult.optionText(),
                        normalizeSuggestedExternalSource(finalResult.suggestedExternalSource()),
                        toRawJson(finalResult)
                );
            } else {
                capture.applyAnalysisFailure(finalFailure, null);
            }
            return toAnalyzeResponse(capture);
        });
        if (response == null) {
            throw new IllegalStateException("분석 결과 저장 중 오류가 발생했습니다.");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PurchaseCaptureAnalyzeResponse getAnalyzeResult(Long userId, Long captureId) {
        return toAnalyzeResponse(getOwnedCapture(userId, captureId));
    }

    private PurchaseCapture getOwnedCapture(Long userId, Long captureId) {
        return purchaseCaptureRepository.findByIdAndUser_Id(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
    }

    private void validateExtractionResult(GeminiPurchaseCaptureExtractionResult result) {
        if (!StringUtils.hasText(result.name())) {
            throw new IllegalStateException("AI 추출 결과에 상품명이 없습니다.");
        }
        if (StringUtils.hasText(result.category()) && StringUtils.hasText(result.itemType())) {
            categoryCatalogService.validateCategoryAndItemType(result.category(), result.itemType());
        }
        if (StringUtils.hasText(result.primaryColor())) {
            categoryCatalogService.validateClothesColors(
                    result.primaryColor(),
                    normalizeSecondaryColors(result.secondaryColors())
            );
        }
        if (result.styles() != null && !result.styles().isEmpty()) {
            categoryCatalogService.validateStyleCodes(result.styles());
        }
        String normalizedExternalSource = normalizeExternalSourceCode(result.suggestedExternalSource());
        if (normalizedExternalSource != null) {
            categoryCatalogService.validateExternalSource(normalizedExternalSource);
        }
    }

    private String normalizeSuggestedExternalSource(String suggestedExternalSource) {
        String normalized = normalizeExternalSourceCode(suggestedExternalSource);
        if (normalized == null) {
            return null;
        }
        try {
            categoryCatalogService.validateExternalSource(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String normalizeExternalSourceCode(String suggestedExternalSource) {
        if (!StringUtils.hasText(suggestedExternalSource)) {
            return null;
        }
        return suggestedExternalSource.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeSecondaryColors(List<String> secondaryColors) {
        return secondaryColors == null ? Collections.emptyList() : secondaryColors;
    }

    private String toColorsJson(List<String> secondaryColors) {
        try {
            return objectMapper.writeValueAsString(normalizeSecondaryColors(secondaryColors));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 추출 결과를 저장하지 못했습니다.");
        }
    }

    private String toStylesJson(List<String> styles) {
        if (styles == null || styles.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(styles);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 추출 결과를 저장하지 못했습니다.");
        }
    }

    private String toRawJson(GeminiPurchaseCaptureExtractionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            return result.toString();
        }
    }

    private PurchaseCaptureAnalyzeResponse toAnalyzeResponse(PurchaseCapture capture) {
        return new PurchaseCaptureAnalyzeResponse(
                capture.getId(),
                capture.getAnalysisStatus(),
                capture.getImageUrl(),
                capture.getFailureMessage(),
                capture.getAnalysisStatus() == com.closetnangam.be.domain.ai.enums.AiAnalysisStatus.FAILED,
                capture.getDraftName(),
                capture.getDraftBrandName(),
                capture.getDraftCategory(),
                capture.getDraftItemType(),
                capture.getDraftPrimaryColor(),
                parseColors(capture.getDraftSecondaryColorsJson()),
                parseStyles(capture.getDraftStylesJson()),
                capture.getDraftOptionText(),
                capture.getDraftExternalSource()
        );
    }

    private List<String> parseColors(String draftSecondaryColorsJson) {
        if (!StringUtils.hasText(draftSecondaryColorsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    draftSecondaryColorsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private List<String> parseStyles(String draftStylesJson) {
        if (!StringUtils.hasText(draftStylesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    draftStylesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

}
