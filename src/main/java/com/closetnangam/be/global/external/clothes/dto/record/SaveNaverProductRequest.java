package com.closetnangam.be.global.external.clothes.dto.record;

import com.closetnangam.be.global.external.clothes.dto.NaverItemRequest;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;

public record SaveNaverProductRequest(
        String productId,
        String brand,
        String category3,
        @JsonProperty("title") String cleanTitle, // JSON의 "title" 값을 cleanTitle 변수에 넣음
        String image,
        String link,
        @JsonProperty("colors") List<ClothingColorDto> colors,
        @JsonProperty("styles") List<ClothesStyleDto> styles
) {}