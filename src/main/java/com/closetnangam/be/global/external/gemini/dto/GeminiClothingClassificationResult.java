package com.closetnangam.be.global.external.gemini.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiClothingClassificationResult(
        @JsonAlias("is_clothing") Boolean isClothing,
        String name,
        @JsonAlias("brand_name") String brandName,
        String category,
        @JsonAlias("item_type") String itemType,
        @JsonAlias("color") String primaryColor,
        @JsonAlias("secondary_colors") List<String> secondaryColors,
        List<String> styles,
        String gender,
        String season
) {
    public boolean isNotClothing() {
        return Boolean.FALSE.equals(isClothing);
    }
}
