package com.closetnangam.be.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ImageStorageService {

    StoredImage storeClothesPhoto(Long userId, MultipartFile file);

    StoredImage storePurchaseCapture(Long userId, MultipartFile file);

    StoredImage storePurchaseCaptureThumbnail(Long userId, Long captureId, int itemIndex, byte[] jpegBytes);

    byte[] readStoredImage(String storedPath);

    byte[] readStoredImage(Path storedPath);

    Path resolveSecureUserImagePath(String subdirectory, Long userId, String filename);

    default boolean supportsLocalImageServing() {
        return false;
    }
}
