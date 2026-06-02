package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.purchase.dto.request.PurchaseCaptureSaveRequest;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureDraftResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureRegistrationResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureUploadResponse;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.domain.purchase.repository.PurchaseCaptureRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseCaptureRegistrationService {

    private final PurchaseCaptureRepository purchaseCaptureRepository;
    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesTagHelper clothesTagHelper;
    private final WardrobeService wardrobeService;
    private final UserRepository userRepository;
    private final LocalImageStorageService localImageStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PurchaseCaptureUploadResponse uploadCapture(Long userId, MultipartFile file) {
        User user = getUser(userId);
        LocalImageStorageService.StoredImage storedImage = localImageStorageService.storePurchaseCapture(userId, file);

        PurchaseCapture capture = purchaseCaptureRepository.save(PurchaseCapture.builder()
                .user(user)
                .imageUrl(storedImage.publicUrl())
                .storedPath(storedImage.storedPath())
                .originalFilename(storedImage.originalFilename())
                .contentType(storedImage.contentType())
                .analysisStatus(AiAnalysisStatus.UPLOADED)
                .build());

        return new PurchaseCaptureUploadResponse(
                capture.getId(),
                capture.getImageUrl(),
                capture.getOriginalFilename(),
                capture.getContentType()
        );
    }

    public PurchaseCaptureDraftResponse getDraft(Long userId, Long captureId) {
        PurchaseCapture capture = getOwnedCapture(userId, captureId);
        return toDraftResponse(capture);
    }

    @Transactional
    public PurchaseCaptureRegistrationResponse savePurchaseCaptureClothes(
            Long userId,
            Long captureId,
            PurchaseCaptureSaveRequest request
    ) {
        clothesTagHelper.validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );
        clothesTagHelper.validateExternalSource(request.externalSource());

        PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_IdForUpdate(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
        if (capture.isAlreadySaved()) {
            throw new IllegalStateException("이미 저장된 구매내역 캡처입니다.");
        }

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Clothes clothes = Clothes.builder()
                .name(request.name())
                .brandName(request.brandName())
                .productCode(request.productCode())
                .imageUrl(capture.getImageUrl())
                .category(request.category())
                .itemType(request.itemType())
                .ownershipStatus(OwnershipStatus.OWNED)
                .infoSource(ClothesInfoSource.PURCHASE_HISTORY)
                .externalSource(request.externalSource())
                .externalProductId(Clothes.EXTERNAL_NONE)
                .externalProductUrl(Clothes.EXTERNAL_NONE)
                .isVerified(request.isVerified())
                .build();

        clothesTagHelper.applyColorTags(clothes, request.primaryColor(), request.secondaryColors());
        clothesTagHelper.applyStyleTags(clothes, request.styles());
        Clothes savedClothes = clothesRepository.save(clothes);

        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.save(WardrobeClothes.builder()
                .wardrobe(wardrobe)
                .clothes(savedClothes)
                .ownershipStatus(OwnershipStatus.OWNED)
                .size(request.size())
                .season(request.season())
                .favorite(request.favorite())
                .userImageUrl(capture.getImageUrl())
                .build());

        capture.markSaved(savedClothes.getId(), wardrobeClothes.getId());

        return new PurchaseCaptureRegistrationResponse(
                wardrobeClothes.getId(),
                savedClothes.getId(),
                capture.getId(),
                wardrobeClothes.getUserImageUrl(),
                wardrobeClothes.getOwnershipStatus(),
                savedClothes.getInfoSource(),
                savedClothes.getExternalSource(),
                wardrobeClothes.getSize(),
                wardrobeClothes.getSeason(),
                wardrobeClothes.getFavorite(),
                ClothesResponse.from(savedClothes, wardrobeClothes)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private PurchaseCapture getOwnedCapture(Long userId, Long captureId) {
        return purchaseCaptureRepository.findByIdAndUser_Id(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
    }

    private PurchaseCaptureDraftResponse toDraftResponse(PurchaseCapture capture) {
        return new PurchaseCaptureDraftResponse(
                capture.getId(),
                capture.getAnalysisStatus(),
                capture.getImageUrl(),
                capture.getFailureMessage(),
                capture.getAnalysisStatus() == AiAnalysisStatus.FAILED,
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
