package com.closetnangam.be.domain.purchase.entity;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;
import com.closetnangam.be.domain.purchase.enums.PurchaseCaptureItemStatus;
import com.closetnangam.be.domain.purchase.support.PurchaseCaptureDraftSupport;
import com.closetnangam.be.domain.purchase.support.PurchaseCaptureDraftSupport.ItemProgressEntry;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.common.entity.BaseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "purchase_captures")
public class PurchaseCapture extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_capture_id")
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

    @Column(name = "draft_primary_color", length = 50)
    private String draftPrimaryColor;

    @Column(name = "draft_secondary_colors_json", columnDefinition = "TEXT")
    private String draftSecondaryColorsJson;

    @Column(name = "draft_styles_json", columnDefinition = "TEXT")
    private String draftStylesJson;

    @Column(name = "draft_option_text", length = 255)
    private String draftOptionText;

    @Column(name = "draft_external_source", length = 50)
    private String draftExternalSource;

    @Column(name = "draft_items_json", columnDefinition = "TEXT")
    private String draftItemsJson;

    @Column(name = "item_progress_json", columnDefinition = "TEXT")
    private String itemProgressJson;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Column(name = "saved_clothes_id")
    private Long savedClothesId;

    @Column(name = "saved_wardrobe_clothes_id")
    private Long savedWardrobeClothesId;

    @Builder
    private PurchaseCapture(
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
            String draftPrimaryColor,
            String draftSecondaryColorsJson,
            String draftStylesJson,
            String draftOptionText,
            String draftExternalSource,
            String draftItemsJson,
            String rawAiResponse
    ) {
        this.analysisStatus = AiAnalysisStatus.SUCCESS;
        this.failureMessage = null;
        this.draftName = draftName;
        this.draftBrandName = draftBrandName;
        this.draftCategory = draftCategory;
        this.draftItemType = draftItemType;
        this.draftPrimaryColor = draftPrimaryColor;
        this.draftSecondaryColorsJson = draftSecondaryColorsJson;
        this.draftStylesJson = draftStylesJson;
        this.draftOptionText = draftOptionText;
        this.draftExternalSource = draftExternalSource;
        this.draftItemsJson = draftItemsJson;
        this.itemProgressJson = null;
        this.rawAiResponse = rawAiResponse;
    }

    public void applyAnalysisFailure(String failureMessage, String rawAiResponse) {
        this.analysisStatus = AiAnalysisStatus.FAILED;
        this.failureMessage = failureMessage;
        this.rawAiResponse = rawAiResponse;
    }

    public void markItemSaved(int itemIndex, Long clothesId, Long wardrobeClothesId, ObjectMapper objectMapper) {
        Map<String, ItemProgressEntry> progress = new LinkedHashMap<>(
                PurchaseCaptureDraftSupport.parseProgress(itemProgressJson, objectMapper)
        );
        progress.put(String.valueOf(itemIndex), new ItemProgressEntry(
                PurchaseCaptureItemStatus.SAVED,
                clothesId,
                wardrobeClothesId
        ));
        this.itemProgressJson = PurchaseCaptureDraftSupport.toProgressJson(progress, objectMapper);
        if (this.savedClothesId == null) {
            this.savedClothesId = clothesId;
            this.savedWardrobeClothesId = wardrobeClothesId;
        }
        refreshCaptureStatus(objectMapper);
    }

    public void markItemSkipped(int itemIndex, ObjectMapper objectMapper) {
        Map<String, ItemProgressEntry> progress = new LinkedHashMap<>(
                PurchaseCaptureDraftSupport.parseProgress(itemProgressJson, objectMapper)
        );
        progress.put(String.valueOf(itemIndex), new ItemProgressEntry(
                PurchaseCaptureItemStatus.SKIPPED,
                null,
                null
        ));
        this.itemProgressJson = PurchaseCaptureDraftSupport.toProgressJson(progress, objectMapper);
        refreshCaptureStatus(objectMapper);
    }

    public boolean isFullyProcessed() {
        return analysisStatus == AiAnalysisStatus.SAVED;
    }

    public boolean hasAnyProcessedItem(ObjectMapper objectMapper) {
        return PurchaseCaptureDraftSupport.hasAnyProcessedItem(
                PurchaseCaptureDraftSupport.parseProgress(itemProgressJson, objectMapper)
        );
    }

    private void refreshCaptureStatus(ObjectMapper objectMapper) {
        int itemCount = PurchaseCaptureDraftSupport.parseRegistrableItems(draftItemsJson, objectMapper).size();
        if (itemCount <= 0) {
            return;
        }
        Map<String, ItemProgressEntry> progress =
                PurchaseCaptureDraftSupport.parseProgress(itemProgressJson, objectMapper);
        if (PurchaseCaptureDraftSupport.isAllItemsProcessed(progress, itemCount)) {
            this.analysisStatus = AiAnalysisStatus.SAVED;
        }
    }
}
