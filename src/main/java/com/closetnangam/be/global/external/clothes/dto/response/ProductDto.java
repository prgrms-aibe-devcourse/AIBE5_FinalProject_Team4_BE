package com.closetnangam.be.global.external.clothes.dto.response;

// 표준화된 외부 상품 DTO
public record ProductDto(
        String title,
        String imageUrl,
        String lprice,
        String link
) {}