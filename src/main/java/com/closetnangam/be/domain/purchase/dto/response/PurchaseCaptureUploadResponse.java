package com.closetnangam.be.domain.purchase.dto.response;

public record PurchaseCaptureUploadResponse(
        Long captureId,
        String imageUrl,
        String originalFilename,
        String contentType
) {
}
