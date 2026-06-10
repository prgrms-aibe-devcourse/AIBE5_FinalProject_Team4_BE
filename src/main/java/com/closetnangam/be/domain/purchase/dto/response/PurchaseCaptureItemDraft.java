package com.closetnangam.be.domain.purchase.dto.response;

import com.closetnangam.be.domain.purchase.enums.PurchaseCaptureItemStatus;

import java.util.List;

public record PurchaseCaptureItemDraft(
        int itemIndex,
        PurchaseCaptureItemStatus status,
        String name,
        String brandName,
        String category,
        String itemType,
        String primaryColor,
        List<String> secondaryColors,
        List<String> styles,
        String gender,
        String season,
        String optionText,
        String suggestedExternalSource,
        String imageUrl
) {
}
