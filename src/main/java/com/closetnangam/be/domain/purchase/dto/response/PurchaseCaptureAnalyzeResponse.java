package com.closetnangam.be.domain.purchase.dto.response;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;

import java.util.List;

public record PurchaseCaptureAnalyzeResponse(
        Long captureId,
        AiAnalysisStatus analysisStatus,
        String previewUrl,
        String failureMessage,
        Boolean aiFailed,
        String name,
        String brandName,
        String category,
        String itemType,
        String gender,
        String primaryColor,
        List<String> secondaryColors,
        List<String> styles,
        String optionText,
        String suggestedExternalSource,
        List<PurchaseCaptureItemDraft> items,
        int pendingItemCount,
        boolean captureCompleted
) {
}
