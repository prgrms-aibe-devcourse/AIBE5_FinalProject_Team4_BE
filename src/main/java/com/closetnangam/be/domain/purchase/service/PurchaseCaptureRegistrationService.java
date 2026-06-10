package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesGender;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.ClothesSeason;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.helper.WardrobeDuplicateGuard;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.purchase.dto.request.PurchaseCaptureSaveRequest;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureDraftResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureRegistrationResponse;
import com.closetnangam.be.domain.purchase.dto.response.PurchaseCaptureUploadResponse;
import com.closetnangam.be.domain.purchase.entity.PurchaseCapture;
import com.closetnangam.be.domain.purchase.enums.PurchaseCaptureItemStatus;
import com.closetnangam.be.domain.purchase.repository.PurchaseCaptureRepository;
import com.closetnangam.be.domain.purchase.support.PurchaseCaptureDraftSupport;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseCaptureRegistrationService {

    private final PurchaseCaptureRepository purchaseCaptureRepository;
    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesTagHelper clothesTagHelper;
    private final WardrobeDuplicateGuard wardrobeDuplicateGuard;
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
        return PurchaseCaptureDraftSupport.toDraftResponse(capture, objectMapper);
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
                request.styles(),
                request.gender()
        );
        clothesTagHelper.validateExternalSource(request.externalSource());
        clothesTagHelper.validateSeasonIfPresent(request.season());

        wardrobeDuplicateGuard.rejectIfAlreadyInWardrobe(
                userId,
                request.brandName(),
                request.name(),
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.productCode()
        );

        PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_IdForUpdate(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
        List<GeminiPurchaseCaptureItem> draftItems =
                PurchaseCaptureDraftSupport.parseRegistrableItems(capture.getDraftItemsJson(), objectMapper);
        int itemIndex = validatePendingItem(capture, request.itemIndex(), draftItems);
        String itemImageUrl = PurchaseCaptureDraftSupport.resolveClothesImageUrl(
                capture.getImageUrl(),
                draftItems,
                itemIndex,
                request.imageUrl()
        );

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Clothes clothes = Clothes.builder()
                .name(request.name())
                .brandName(request.brandName())
                .productCode(request.productCode())
                .imageUrl(itemImageUrl)
                .category(request.category())
                .itemType(request.itemType())
                .gender(ClothesGender.fromCode(request.gender()))
                .season(ClothesSeason.fromCodeOrDefault(request.season()))
                .clothesInfoSource(ClothesInfoSource.PURCHASE_HISTORY)
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
                .favorite(request.favorite())
                .userImageUrl(itemImageUrl)
                .build());

        capture.markItemSaved(itemIndex, savedClothes.getId(), wardrobeClothes.getId(), objectMapper);
        PurchaseCaptureDraftResponse draft = PurchaseCaptureDraftSupport.toDraftResponse(capture, objectMapper);

        return new PurchaseCaptureRegistrationResponse(
                wardrobeClothes.getId(),
                savedClothes.getId(),
                capture.getId(),
                itemIndex,
                wardrobeClothes.getUserImageUrl(),
                wardrobeClothes.getOwnershipStatus(),
                savedClothes.getClothesInfoSource(),
                savedClothes.getExternalSource(),
                wardrobeClothes.getSize(),
                savedClothes.getSeason() != null ? savedClothes.getSeason().name() : null,
                wardrobeClothes.getFavorite(),
                draft.pendingItemCount(),
                draft.captureCompleted(),
                ClothesResponse.from(savedClothes, wardrobeClothes)
        );
    }

    @Transactional
    public PurchaseCaptureDraftResponse skipPurchaseCaptureItem(Long userId, Long captureId, int itemIndex) {
        PurchaseCapture capture = purchaseCaptureRepository.findByIdAndUser_IdForUpdate(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
        List<GeminiPurchaseCaptureItem> draftItems =
                PurchaseCaptureDraftSupport.parseRegistrableItems(capture.getDraftItemsJson(), objectMapper);
        validatePendingItem(capture, itemIndex, draftItems);
        capture.markItemSkipped(itemIndex, objectMapper);
        return PurchaseCaptureDraftSupport.toDraftResponse(capture, objectMapper);
    }

    private int validatePendingItem(
            PurchaseCapture capture,
            Integer requestedItemIndex,
            List<GeminiPurchaseCaptureItem> items
    ) {
        if (capture.isFullyProcessed()) {
            throw new IllegalStateException("이미 처리가 완료된 구매내역 캡처입니다.");
        }
        if (capture.getAnalysisStatus() != AiAnalysisStatus.SUCCESS) {
            throw new IllegalStateException("분석이 완료된 구매내역 캡처만 처리할 수 있습니다.");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("저장할 구매내역 상품이 없습니다.");
        }
        int itemIndex = PurchaseCaptureDraftSupport.resolveItemIndex(requestedItemIndex, items.size());
        PurchaseCaptureItemStatus status = PurchaseCaptureDraftSupport.resolveItemStatus(
                PurchaseCaptureDraftSupport.parseProgress(capture.getItemProgressJson(), objectMapper),
                itemIndex
        );
        if (status == PurchaseCaptureItemStatus.SAVED) {
            throw new IllegalStateException("이미 저장된 상품입니다. itemIndex=" + itemIndex);
        }
        if (status == PurchaseCaptureItemStatus.SKIPPED) {
            throw new IllegalStateException("이미 건너뛴 상품입니다. itemIndex=" + itemIndex);
        }
        return itemIndex;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private PurchaseCapture getOwnedCapture(Long userId, Long captureId) {
        return purchaseCaptureRepository.findByIdAndUser_Id(captureId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 구매내역 캡처를 찾을 수 없습니다."));
    }
}
