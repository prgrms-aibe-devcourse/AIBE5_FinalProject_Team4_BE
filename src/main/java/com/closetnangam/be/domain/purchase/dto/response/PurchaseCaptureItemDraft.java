package com.closetnangam.be.domain.purchase.dto.response;

import java.util.List;

public record PurchaseCaptureItemDraft(
        int itemIndex,
        String name,
        String brandName,
        String category,
        String itemType,
        String primaryColor,
        List<String> secondaryColors,
        List<String> styles,
        String optionText,
        String suggestedExternalSource,
        String imageUrl
) {
}
