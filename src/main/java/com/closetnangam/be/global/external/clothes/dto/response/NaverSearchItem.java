package com.closetnangam.be.global.external.clothes.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 네이버 응답 내부의 개별 상품 정보 (내부 클래스)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverSearchItem(

        @JsonProperty("title") String title,
        @JsonProperty("link") String link,
        @JsonProperty("image") String image,
        @JsonProperty("lprice") String lprice
) {}
