package com.closetnangam.be.global.external.gemini.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiPurchaseCaptureExtractionResult(
        String name,
        @JsonAlias("brand_name") String brandName,
        String category,
        @JsonAlias("item_type") String itemType,
        @JsonAlias("color") String primaryColor,
        @JsonAlias("secondary_colors") List<String> secondaryColors,
        List<String> styles,
        @JsonAlias("option_text") String optionText,
        @JsonAlias("suggested_external_source") String suggestedExternalSource,
        @JsonAlias("image_url") String imageUrl,
        @JsonAlias("thumbnail_region") GeminiThumbnailRegion thumbnailRegion,
        @JsonAlias("order_status") String orderStatus,
        List<GeminiPurchaseCaptureItem> items
) {
}
