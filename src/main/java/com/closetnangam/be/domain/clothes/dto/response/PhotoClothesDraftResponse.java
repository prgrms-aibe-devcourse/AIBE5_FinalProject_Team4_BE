package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.ai.enums.AiAnalysisStatus;

import java.util.List;

public record PhotoClothesDraftResponse(
        Long photoId,
        AiAnalysisStatus analysisStatus,
        String previewUrl,
        String failureMessage,
        Boolean aiFailed,
        String name,
        String brandName,
        String category,
        String itemType,
        String color,
        List<String> styles
) {
}
