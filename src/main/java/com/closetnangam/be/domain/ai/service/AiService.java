package com.closetnangam.be.domain.ai.service;

import com.closetnangam.be.domain.ai.dto.response.AiAnalyzeResponse;
import com.closetnangam.be.domain.ai.entity.ClothingAiPhoto;
import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.ai.repository.ClothingAiPhotoRepository;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
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

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ClothingAiPhotoRepository clothingAiPhotoRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final GeminiService geminiService;
    private final LocalImageStorageService localImageStorageService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public AiService(
            ClothingAiPhotoRepository clothingAiPhotoRepository,
            CategoryCatalogService categoryCatalogService,
            GeminiService geminiService,
            LocalImageStorageService localImageStorageService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.clothingAiPhotoRepository = clothingAiPhotoRepository;
        this.categoryCatalogService = categoryCatalogService;
        this.geminiService = geminiService;
        this.localImageStorageService = localImageStorageService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // 트랜잭션을 외부 API 호출 구간에서 분리해 DB 커넥션 점유를 최소화
    public AiAnalyzeResponse analyzeClothingPhoto(Long userId, Long photoId) {
        // Phase 1: 소유권 확인 + ANALYZING 상태 기록 (단기 트랜잭션)
        // FOR UPDATE: 저장 API와 동일한 row lock 규칙으로 직렬화하여
        // 저장 트랜잭션이 SAVED로 커밋한 뒤 markAnalyzing()이 그 상태를 덮어쓰는 것을 방지
        StoredImageAnalysisContext ctx = transactionTemplate.execute(status -> {
            ClothingAiPhoto photo = clothingAiPhotoRepository.findByIdAndUser_IdForUpdate(photoId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
            if (photo.isAlreadySaved()) {
                throw new IllegalStateException("이미 저장된 사진은 다시 분석할 수 없습니다.");
            }
            photo.markAnalyzing();
            return new StoredImageAnalysisContext(photo.getStoredPath(), photo.getContentType());
        });
        if (ctx == null) {
            throw new IllegalStateException("분석 준비 중 오류가 발생했습니다.");
        }

        // Phase 2: 파일 읽기 + Gemini API 호출 (DB 커넥션 미보유)
        // Exception을 포함한 모든 예외를 포착해 photo가 ANALYZING 상태로 영구 고착되는 것을 방지
        GeminiClothingClassificationResult result = null;
        String failureMessage = null;
        try {
            byte[] imageBytes = localImageStorageService.readStoredImage(ctx.storedPath());
            result = geminiService.classifyClothingImage(
                    imageBytes,
                    ctx.contentType(),
                    categoryCatalogService.getAiClassificationGuide()
            );
            validateClassificationResult(result);
        } catch (IllegalArgumentException e) {
            // 파일 미존재 등 입력 문제
            log.warn("[AI분석] 이미지 파일을 읽지 못했습니다. userId={}, photoId={}: {}", userId, photoId, e.getMessage());
            failureMessage = StringUtils.hasText(e.getMessage())
                    ? e.getMessage()
                    : "업로드된 이미지를 찾을 수 없습니다.";
        } catch (IllegalStateException e) {
            // Gemini API 실패, 응답 파싱 실패, AI 결과 불충분(유효하지 않은 category/color/style 포함)
            log.warn("[AI분석] AI 분석 실패. userId={}, photoId={}: {}", userId, photoId, e.getMessage());
            failureMessage = resolveAnalysisFailureMessage(e);
        } catch (Exception e) {
            // 예기치 않은 런타임 오류도 FAILED로 처리해 ANALYZING 상태 고착 방지
            log.error("[AI분석] 예상치 못한 오류. userId={}, photoId={}", userId, photoId, e);
            failureMessage = "AI 분석 중 예기치 않은 오류가 발생했습니다. 직접 입력해 주세요.";
        }

        // Phase 3: 분석 결과 저장 (단기 트랜잭션)
        final GeminiClothingClassificationResult finalResult = result;
        final String finalFailure = failureMessage;
        AiAnalyzeResponse response = transactionTemplate.execute(status -> {
            // FOR UPDATE: 저장 API의 row lock과 동일한 규칙으로 직렬화하여
            // 저장 트랜잭션이 먼저 커밋된 경우 SAVED 상태를 반드시 확인하고 덮어쓰지 않도록 보장
            ClothingAiPhoto photo = clothingAiPhotoRepository.findByIdAndUser_IdForUpdate(photoId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
            // 분석 대기 중 사용자가 저장을 완료한 경우 분석 결과로 SAVED 상태를 덮어쓰지 않음
            if (photo.isAlreadySaved()) {
                return toAnalyzeResponse(photo);
            }
            if (finalResult != null) {
                photo.applyAnalysisSuccess(
                        finalResult.name(),
                        finalResult.brandName(),
                        finalResult.category(),
                        finalResult.itemType(),
                        categoryCatalogService.resolveGenderOrDefault(finalResult.gender()).name(),
                        finalResult.primaryColor(),
                        toColorsJson(finalResult.secondaryColors()),
                        toStylesJson(finalResult.styles()),
                        toRawJson(finalResult)
                );
            } else {
                photo.applyAnalysisFailure(finalFailure, null);
            }
            return toAnalyzeResponse(photo);
        });
        if (response == null) {
            throw new IllegalStateException("분석 결과 저장 중 오류가 발생했습니다.");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public AiAnalyzeResponse getAnalyzeResult(Long userId, Long photoId) {
        return toAnalyzeResponse(getOwnedPhoto(userId, photoId));
    }

    private ClothingAiPhoto getOwnedPhoto(Long userId, Long photoId) {
        return clothingAiPhotoRepository.findByIdAndUser_Id(photoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
    }

    private void validateClassificationResult(GeminiClothingClassificationResult result) {
        if (!StringUtils.hasText(result.name())
                || !StringUtils.hasText(result.category())
                || !StringUtils.hasText(result.itemType())
                || !StringUtils.hasText(result.primaryColor())
                || result.styles() == null
                || result.styles().isEmpty()) {
            throw new IllegalStateException("AI 판별 결과가 충분하지 않습니다.");
        }
        try {
            categoryCatalogService.validateCategoryAndItemType(result.category(), result.itemType());
            categoryCatalogService.validateClothesColors(result.primaryColor(), normalizeSecondaryColors(result.secondaryColors()));
            categoryCatalogService.validateStyleCodes(result.styles());
            categoryCatalogService.validateGenderCode(
                    categoryCatalogService.resolveGenderOrDefault(result.gender()).name()
            );
        } catch (IllegalArgumentException e) {
            // AI가 유효하지 않은 category/color/style 코드를 반환한 경우
            throw new IllegalStateException("AI가 유효하지 않은 분류 결과를 반환했습니다: " + e.getMessage(), e);
        }
    }

    private String resolveAnalysisFailureMessage(IllegalStateException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return "AI가 옷 이미지를 분석하지 못했습니다. 직접 입력해 주세요.";
        }
        if (message.contains("API 키")
                || message.contains("API 호출")
                || message.contains("응답을 해석")
                || message.contains("판별 결과가 충분하지 않")
                || message.contains("유효하지 않은 분류")) {
            return message;
        }
        return "AI가 옷 이미지를 분석하지 못했습니다. 직접 입력해 주세요.";
    }

    private List<String> normalizeSecondaryColors(List<String> secondaryColors) {
        return secondaryColors == null ? Collections.emptyList() : secondaryColors;
    }

    private String toColorsJson(List<String> secondaryColors) {
        try {
            return objectMapper.writeValueAsString(normalizeSecondaryColors(secondaryColors));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 판별 결과를 저장하지 못했습니다.");
        }
    }

    private String toStylesJson(List<String> styles) {
        try {
            return objectMapper.writeValueAsString(styles);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 판별 결과를 저장하지 못했습니다.");
        }
    }

    private String toRawJson(GeminiClothingClassificationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            return result.toString();
        }
    }

    private AiAnalyzeResponse toAnalyzeResponse(ClothingAiPhoto photo) {
        List<String> styles = parseStyles(photo.getDraftStylesJson());
        List<String> secondaryColors = parseColors(photo.getDraftSecondaryColorsJson());
        boolean aiFailed = photo.getAnalysisStatus() == AiAnalysisStatus.FAILED;

        return new AiAnalyzeResponse(
                photo.getId(),
                photo.getAnalysisStatus(),
                photo.getImageUrl(),
                photo.getFailureMessage(),
                aiFailed,
                photo.getDraftName(),
                photo.getDraftBrandName(),
                photo.getDraftCategory(),
                photo.getDraftItemType(),
                photo.getDraftPrimaryColor(),
                secondaryColors,
                styles,
                photo.getDraftGender()
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
