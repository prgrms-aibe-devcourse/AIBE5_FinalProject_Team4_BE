package com.closetnangam.be.domain.clothes.dto.response;

import com.closetnangam.be.domain.clothes.enums.ClothesRegistrationMethod;

public record ClothesRegistrationMethodResponse(
        String code,
        String name,
        String description
) {

    public static ClothesRegistrationMethodResponse from(ClothesRegistrationMethod method) {
        return new ClothesRegistrationMethodResponse(
                method.name(),
                method.getLabel(),
                method.getDescription()
        );
    }
}
