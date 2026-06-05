package com.closetnangam.be.global.external.clothes.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverSearchResponse(
        @JsonProperty("total") int total,
        @JsonProperty("display") int display,
        @JsonProperty("items") List<NaverSearchItem> items // 여기에 정확히 명시!
) {}