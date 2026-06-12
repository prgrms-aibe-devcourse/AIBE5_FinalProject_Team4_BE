package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Locale;

@Tag(name = "Image", description = "의류 이미지 서빙 API (로컬 저장소 전용, 인증 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local", matchIfMissing = true)
public class ImageController {

    private static final String CLOTHES_SUBDIRECTORY = "clothes";
    private static final String PURCHASE_CAPTURES_SUBDIRECTORY = "purchase-captures";

    private final LocalImageStorageService localImageStorageService;

    @Operation(
            summary = "의류 사진 조회",
            description = "업로드된 의류 사진을 소유자 본인에게만 반환합니다. 로그인이 필요합니다."
    )
    @GetMapping("/clothes/{userId}/{filename}")
    public ResponseEntity<byte[]> getClothesImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        SecurityUtils.verifyOwnership(userId);
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
