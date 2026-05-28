package com.closetnangam.be.global.external.gemini.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiClothingClassificationResult(
        String name,
        String brandName,
        String category,
        String itemType,
        @JsonAlias("color") String primaryColor,
        List<String> secondaryColors,
        List<String> styles
) {
}
