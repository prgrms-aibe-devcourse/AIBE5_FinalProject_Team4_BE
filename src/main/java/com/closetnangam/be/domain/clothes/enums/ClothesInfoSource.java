package com.closetnangam.be.domain.clothes.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClothesInfoSource {

    PHOTO("사진 기반 등록"),
    PURCHASE_HISTORY("구매 내역 기반 등록"),
    EXTERNAL_SHOPPING("외부 쇼핑몰 등록");

    private final String label;
}
