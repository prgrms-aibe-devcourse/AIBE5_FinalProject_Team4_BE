package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.ai.entity.ClothingAiPhoto;
import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.ai.repository.ClothingAiPhotoRepository;
import com.closetnangam.be.domain.clothes.dto.request.PhotoClothesSaveRequest;
import com.closetnangam.be.domain.clothes.helper.ClothesTagHelper;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesDraftResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoClothesRegistrationResponse;
import com.closetnangam.be.domain.clothes.dto.response.PhotoUploadResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ClothesInfoSource;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
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
public class PhotoClothesRegistrationService {

    private final ClothingAiPhotoRepository clothingAiPhotoRepository;
    private final ClothesRepository clothesRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final ClothesTagHelper clothesTagHelper;
    private final WardrobeService wardrobeService;
    private final UserRepository userRepository;
    private final LocalImageStorageService localImageStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PhotoUploadResponse uploadPhoto(Long userId, MultipartFile file) {
        User user = getUser(userId);
        LocalImageStorageService.StoredImage storedImage = localImageStorageService.storeClothesPhoto(userId, file);

        ClothingAiPhoto photo = clothingAiPhotoRepository.save(ClothingAiPhoto.builder()
                .user(user)
                .imageUrl(storedImage.publicUrl())
                .storedPath(storedImage.storedPath())
                .originalFilename(storedImage.originalFilename())
                .contentType(storedImage.contentType())
                .analysisStatus(AiAnalysisStatus.UPLOADED)
                .build());

        return new PhotoUploadResponse(
                photo.getId(),
                photo.getImageUrl(),
                photo.getOriginalFilename(),
                photo.getContentType()
        );
    }

    public PhotoClothesDraftResponse getDraft(Long userId, Long photoId) {
        ClothingAiPhoto photo = getOwnedPhoto(userId, photoId);
        return toDraftResponse(photo);
    }

    @Transactional
    public PhotoClothesRegistrationResponse savePhotoClothes(
            Long userId,
            Long photoId,
            PhotoClothesSaveRequest request
    ) {
        validateClassification(
                request.category(),
                request.itemType(),
                request.primaryColor(),
                request.secondaryColors(),
                request.styles()
        );

        // 비관적 락으로 동시 저장 요청 직렬화: 두 트랜잭션이 동시에 isAlreadySaved()==false를 보고 중복 생성하는 경쟁 조건 방지
        ClothingAiPhoto photo = clothingAiPhotoRepository.findByIdAndUser_IdForUpdate(photoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
        if (photo.isAlreadySaved()) {
            throw new IllegalStateException("이미 저장된 사진입니다.");
        }

        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Clothes clothes = Clothes.builder()
                .name(request.name())
                .brandName(request.brandName())
                .productCode(request.productCode())
                .imageUrl(photo.getImageUrl())
                .category(request.category())
                .itemType(request.itemType())
                .ownershipStatus(OwnershipStatus.OWNED)
                .infoSource(ClothesInfoSource.PHOTO)
                .externalSource(Clothes.EXTERNAL_NONE)
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
                .userImageUrl(photo.getImageUrl())
                .build());

        photo.markSaved(savedClothes.getId(), wardrobeClothes.getId());

        return new PhotoClothesRegistrationResponse(
                wardrobeClothes.getId(),
                savedClothes.getId(),
                photo.getId(),
                wardrobeClothes.getUserImageUrl(),
                wardrobeClothes.getOwnershipStatus(),
                savedClothes.getInfoSource(),
                wardrobeClothes.getSize(),
                wardrobeClothes.getSeason(),
                wardrobeClothes.getFavorite(),
                com.closetnangam.be.domain.clothes.dto.response.ClothesResponse.from(savedClothes, wardrobeClothes)
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private ClothingAiPhoto getOwnedPhoto(Long userId, Long photoId) {
        return clothingAiPhotoRepository.findByIdAndUser_Id(photoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("업로드한 사진을 찾을 수 없습니다."));
    }

    private PhotoClothesDraftResponse toDraftResponse(ClothingAiPhoto photo) {
        List<String> styles = parseStyles(photo.getDraftStylesJson());
        boolean aiFailed = photo.getAnalysisStatus() == AiAnalysisStatus.FAILED;

        return new PhotoClothesDraftResponse(
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
                parseColors(photo.getDraftSecondaryColorsJson()),
                styles
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

    private void validateClassification(
            String category,
            String itemType,
            String primaryColor,
            List<String> secondaryColors,
            List<String> styles
    ) {
        clothesTagHelper.validateClassification(category, itemType, primaryColor, secondaryColors, styles);
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
