package com.closetnangam.be.global.storage;

public record StoredImage(
        String publicUrl,
        String storedPath,
        String contentType,
        String originalFilename
) {
}
