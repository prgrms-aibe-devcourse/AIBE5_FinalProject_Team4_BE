package com.closetnangam.be.domain.clothes.dto.response;

import java.util.List;

public record ClothesRegistrationMethodsResponse(
        String title,
        String description,
        List<ClothesRegistrationMethodResponse> methods
) {
}
