package com.closetnangam.be.domain.clothes.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClothesRegistrationMethod {

    PURCHASE_HISTORY(
            "구매내역 기반 등록",
            "구매내역 캡처를 업로드해 상품 정보를 추출한 뒤 옷장에 등록합니다."
    ),
    PHOTO(
            "사진 기반 등록",
            "옷 사진을 촬영하거나 업로드해 등록합니다."
    );

    private final String label;
    private final String description;

    public static ClothesRegistrationMethod fromCode(String code) {
        try {
            return ClothesRegistrationMethod.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 등록 방식 코드입니다: " + code, e);
        }
    }
}
