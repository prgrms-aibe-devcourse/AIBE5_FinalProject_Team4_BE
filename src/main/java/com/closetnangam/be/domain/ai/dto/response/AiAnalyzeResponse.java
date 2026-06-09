package com.closetnangam.be.domain.ai.dto.response;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;

import java.util.List;

public record AiAnalyzeResponse(
        Long photoId,
        AiAnalysisStatus analysisStatus,
        String previewUrl,
        String failureMessage,
        Boolean aiFailed,
        String name,
        String brandName,
        String category,
        String itemType,
        String primaryColor,
        List<String> secondaryColors,
        List<String> styles,
        String gender
) {
}
