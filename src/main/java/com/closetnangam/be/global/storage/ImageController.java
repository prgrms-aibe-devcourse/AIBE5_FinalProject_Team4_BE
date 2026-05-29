package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.common.util.SecurityUtils;
import com.closetnangam.be.global.config.StorageProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.Locale;

@Tag(name = "Image", description = "의류 이미지 서빙 API (인증 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final LocalImageStorageService localImageStorageService;
    private final StorageProperties storageProperties;

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

        // 경로 순회(path traversal) 방지: filename에 경로 구분자·상위 디렉터리 탐색 포함 금지
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        String storedPath = Paths.get(
                storageProperties.getLocal().getBasePath(),
                "clothes",
                String.valueOf(userId),
                filename
        ).toString();

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
        // jpg, jpeg — 기본값
        return MediaType.IMAGE_JPEG;
    }
}
