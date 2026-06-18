package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Locale;

@Tag(name = "Image", description = "의류 이미지 서빙 API (인증 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private static final String CLOTHES_SUBDIRECTORY = "clothes";
    private static final String PURCHASE_CAPTURES_SUBDIRECTORY = "purchase-captures";
    private static final String FEED_SUBDIRECTORY = "feed";
    private static final String PROFILE_SUBDIRECTORY = "profile";

    private final LocalImageStorageService localImageStorageService;

    @Operation(
            summary = "의류 사진 조회",
            description = """
                    CLOTHES 공통 옷 이미지를 반환합니다. 로그인한 사용자는 피드·코디 등에 노출된 \
                    다른 사용자의 옷 사진도 조회할 수 있습니다. path의 userId는 업로드 소유자 ID입니다."""
    )
    @GetMapping("/clothes/{userId}/{filename}")
    public ResponseEntity<byte[]> getClothesImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(CLOTHES_SUBDIRECTORY, userId, filename);
    }

    @Operation(
            summary = "구매내역 캡처 조회",
            description = "업로드된 구매내역 캡처를 소유자 본인에게만 반환합니다. 로그인이 필요합니다."
    )
    @GetMapping("/purchase-captures/{userId}/{filename}")
    public ResponseEntity<byte[]> getPurchaseCaptureImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        SecurityUtils.verifyOwnership(userId);
        return serveUserImage(PURCHASE_CAPTURES_SUBDIRECTORY, userId, filename);
    }

    @Operation(
            summary = "피드 사진 조회",
            description = """
                    공개 피드에 포함된 사진을 반환합니다. 로그인한 사용자는 다른 사용자가 업로드한 \
                    피드 사진도 조회할 수 있습니다. path의 userId는 업로드 소유자 ID입니다."""
    )
    @GetMapping("/feed/{userId}/{filename}")
    public ResponseEntity<byte[]> getFeedImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(FEED_SUBDIRECTORY, userId, filename);
    }

    @Operation(
            summary = "프로필 이미지 조회",
            description = """
                    사용자 프로필 이미지를 반환합니다. 룩피드와 마이페이지에서 노출되는 공개 프로필 이미지이며, \
                    path의 userId는 업로드 소유자 ID입니다."""
    )
    @GetMapping("/profile/{userId}/{filename}")
    public ResponseEntity<byte[]> getProfileImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(PROFILE_SUBDIRECTORY, userId, filename);
    }

    private ResponseEntity<byte[]> serveUserImage(String subdirectory, Long userId, String filename) {
        Path storedPath = localImageStorageService.resolveSecureUserImagePath(subdirectory, userId, filename);
        byte[] imageBytes = localImageStorageService.readStoredImage(storedPath);

        return ResponseEntity.ok()
                .contentType(resolveMediaType(filename))
                .body(imageBytes);
    }

    private MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
