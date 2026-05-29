package com.closetnangam.be.domain.clothes.dto.response;

public record PhotoUploadResponse(
        Long photoId,
        String previewUrl,
        String originalFilename,
        String contentType
) {
}
