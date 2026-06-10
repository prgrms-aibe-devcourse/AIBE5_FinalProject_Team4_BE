package com.closetnangam.be.domain.ai.entity;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clothing_ai_photos")
public class ClothingAiPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_ai_photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private AiAnalysisStatus analysisStatus;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "draft_name", length = 255)
    private String draftName;

    @Column(name = "draft_brand_name", length = 100)
    private String draftBrandName;

    @Column(name = "draft_category", length = 50)
    private String draftCategory;

    @Column(name = "draft_item_type", length = 50)
    private String draftItemType;

    @Column(name = "draft_gender", length = 20)
    private String draftGender;

    @Column(name = "draft_color", length = 50)
    private String draftPrimaryColor;

    @Column(name = "draft_secondary_colors_json", columnDefinition = "TEXT")
    private String draftSecondaryColorsJson;

    @Column(name = "draft_styles_json", columnDefinition = "TEXT")
    private String draftStylesJson;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Column(name = "saved_clothes_id")
    private Long savedClothesId;

    @Column(name = "saved_wardrobe_clothes_id")
    private Long savedWardrobeClothesId;

    @Builder
    private ClothingAiPhoto(
            User user,
            String imageUrl,
            String storedPath,
            String originalFilename,
            String contentType,
            AiAnalysisStatus analysisStatus
    ) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.storedPath = storedPath;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.analysisStatus = analysisStatus;
    }

    public void markAnalyzing() {
        this.analysisStatus = AiAnalysisStatus.ANALYZING;
        this.failureMessage = null;
    }

    public void applyAnalysisSuccess(
            String draftName,
            String draftBrandName,
            String draftCategory,
            String draftItemType,
            String draftGender,
            String draftPrimaryColor,
            String draftSecondaryColorsJson,
            String draftStylesJson,
            String rawAiResponse
    ) {
        this.analysisStatus = AiAnalysisStatus.SUCCESS;
        this.failureMessage = null;
        this.draftName = draftName;
        this.draftBrandName = draftBrandName;
        this.draftCategory = draftCategory;
        this.draftItemType = draftItemType;
        this.draftGender = draftGender;
        this.draftPrimaryColor = draftPrimaryColor;
        this.draftSecondaryColorsJson = draftSecondaryColorsJson;
        this.draftStylesJson = draftStylesJson;
        this.rawAiResponse = rawAiResponse;
    }

    public void applyAnalysisFailure(String failureMessage, String rawAiResponse) {
        this.analysisStatus = AiAnalysisStatus.FAILED;
        this.failureMessage = failureMessage;
        this.rawAiResponse = rawAiResponse;
    }

    public void markSaved(Long clothesId, Long wardrobeClothesId) {
        this.analysisStatus = AiAnalysisStatus.SAVED;
        this.savedClothesId = clothesId;
        this.savedWardrobeClothesId = wardrobeClothesId;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public boolean isAlreadySaved() {
        return analysisStatus == AiAnalysisStatus.SAVED;
    }
}
