package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Locale;

@Tag(name = "Image", description = "이미지 서빙 API — clothes·purchase-captures: 인증 필요 / feed·profile: 공개")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private static final String CLOTHES_SUBDIRECTORY = "clothes";
    private static final String PURCHASE_CAPTURES_SUBDIRECTORY = "purchase-captures";
    private static final String FEED_SUBDIRECTORY = "feed";
    private static final String PROFILE_SUBDIRECTORY = "profile";

    private final ImageStorageService imageStorageService;
    private final RestTemplate restTemplate;

    @Operation(
            summary = "외부 이미지 프록시",
            description = """
                    외부 이미지(예: pstatic.net)를 프록시하여 반환합니다. \
                    FE의 Canvas CORS 문제를 해결하기 위해 사용합니다. \
                    pstatic.net 도메인만 허용됩니다."""
    )
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(
            @Parameter(description = "외부 이미지 URL", example = "https://shopping-phinf.pstatic.net/...")
            @RequestParam String url
    ) {
        if (!url.contains("pstatic.net")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    null,
                    byte[].class
                );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok()
                        .contentType(response.getHeaders().getContentType())
                        .contentLength(response.getHeaders().getContentLength())
                        .body(response.getBody());
            }
        } catch (Exception e) {
            // 외부 이미지 로드 실패 시 404
        }

        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "의류 사진 조회 (인증 필요)",
            description = """
                    CLOTHES 옷 이미지를 반환합니다. 로그인한 사용자라면 피드·코디 등에 노출된 \
                    다른 사용자의 옷 사진도 조회할 수 있습니다. path의 userId는 업로드 소유자 ID입니다.
                    <img> 태그는 쿠키를 자동 전송하지 않으므로, FE는 인증 fetch + blob URL 변환을 사용해야 합니다."""
    )
    @GetMapping("/clothes/{userId}/{filename}")
    public ResponseEntity<byte[]> getClothesImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(CLOTHES_SUBDIRECTORY, userId, filename);
    }

    @Operation(
            summary = "구매내역 캡처 이미지 조회 (소유자 전용)",
            description = """
                    업로드된 구매내역 캡처 이미지를 반환합니다. 로그인이 필요하며 \
                    본인 소유 이미지만 조회할 수 있습니다."""
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
            summary = "피드 사진 조회 (공개)",
            description = """
                    공개 피드에 포함된 사진을 반환합니다. 비로그인 사용자도 접근 가능합니다. \
                    path의 userId는 업로드 소유자 ID입니다."""
    )
    @GetMapping("/feed/{userId}/{filename}")
    public ResponseEntity<byte[]> getFeedImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(FEED_SUBDIRECTORY, userId, filename);
    }

    @Operation(
            summary = "프로필 이미지 조회 (공개)",
            description = """
                    사용자 프로필 이미지를 반환합니다. 룩피드·마이페이지에 노출되는 공개 이미지이며 \
                    비로그인 사용자도 접근 가능합니다. path의 userId는 업로드 소유자 ID입니다."""
    )
    @GetMapping("/profile/{userId}/{filename}")
    public ResponseEntity<byte[]> getProfileImage(
            @PathVariable Long userId,
            @PathVariable String filename
    ) {
        return serveUserImage(PROFILE_SUBDIRECTORY, userId, filename);
    }

    private ResponseEntity<byte[]> serveUserImage(String subdirectory, Long userId, String filename) {
        String storedPath = imageStorageService.resolveUserImageStoredPath(subdirectory, userId, filename);
        byte[] imageBytes = imageStorageService.readStoredImage(storedPath);

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
