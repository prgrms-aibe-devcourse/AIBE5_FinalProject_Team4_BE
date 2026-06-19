package com.closetnangam.be.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ImageStorageService {

    StoredImage storeClothesPhoto(Long userId, MultipartFile file);

    StoredImage storePurchaseCapture(Long userId, MultipartFile file);

    StoredImage storePurchaseCaptureThumbnail(Long userId, Long captureId, int itemIndex, byte[] jpegBytes);

    StoredImage storeFeedPhoto(Long userId, MultipartFile file);

    StoredImage storeProfileImage(Long userId, MultipartFile file);

    byte[] readStoredImage(String storedPath);

    byte[] readStoredImage(Path storedPath);

    /**
     * 브라우저가 요청한 이미지 URL 조각을 실제 저장소 경로로 변환합니다.
     *
     * <p>local은 디스크 경로, S3는 object key를 반환합니다. 컨트롤러가 저장소 종류를 몰라도
     * 같은 인증/응답 흐름으로 이미지를 서빙할 수 있게 두 저장소의 경로 해석을 이 인터페이스로 모읍니다.</p>
     */
    String resolveUserImageStoredPath(String subdirectory, Long userId, String filename);

    Path resolveSecureUserImagePath(String subdirectory, Long userId, String filename);

    default boolean supportsLocalImageServing() {
        return false;
    }
}
