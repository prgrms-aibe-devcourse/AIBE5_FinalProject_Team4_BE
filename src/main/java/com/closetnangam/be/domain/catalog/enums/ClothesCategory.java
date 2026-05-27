package com.closetnangam.be.domain.catalog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClothesCategory {

    TOP("상의"),
    BOTTOM("하의"),
    OUTER("아우터"),
    SHOES("신발");

    private final String label;

    public static ClothesCategory fromCode(String code) {
        return ClothesCategory.valueOf(code);
    }
}
