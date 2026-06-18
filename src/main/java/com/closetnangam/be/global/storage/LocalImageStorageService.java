package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalImageStorageService {

    private static final int IMAGE_HEADER_SIZE = 12;

    private final StorageProperties storageProperties;

    public StoredImage storeClothesPhoto(Long userId, MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = Paths.get(storageProperties.getLocal().getBasePath(), "clothes", String.valueOf(userId));
        Path targetPath = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String publicUrl = storageProperties.getLocal().getBaseUrl()
                + "/clothes/" + userId + "/" + storedFileName;

        return new StoredImage(publicUrl, targetPath.toString(), file.getContentType(), file.getOriginalFilename());
    }

    public StoredImage storePurchaseCapture(Long userId, MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = Paths.get(storageProperties.getLocal().getBasePath(), "purchase-captures", String.valueOf(userId));
        Path targetPath = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String publicUrl = storageProperties.getLocal().getBaseUrl()
                + "/purchase-captures/" + userId + "/" + storedFileName;

        return new StoredImage(publicUrl, targetPath.toString(), file.getContentType(), file.getOriginalFilename());
    }

    public StoredImage storeFeedPhoto(Long userId, MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = Paths.get(storageProperties.getLocal().getBasePath(), "feed", String.valueOf(userId));
        Path targetPath = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String publicUrl = storageProperties.getLocal().getBaseUrl()
                + "/feed/" + userId + "/" + storedFileName;

        return new StoredImage(publicUrl, targetPath.toString(), file.getContentType(), file.getOriginalFilename());
    }

    public StoredImage storeProfileImage(Long userId, MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = Paths.get(storageProperties.getLocal().getBasePath(), "profile", String.valueOf(userId));
        Path targetPath = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String publicUrl = storageProperties.getLocal().getBaseUrl()
                + "/profile/" + userId + "/" + storedFileName;

        return new StoredImage(publicUrl, targetPath.toString(), file.getContentType(), file.getOriginalFilename());
    }

    public byte[] readStoredImage(String storedPath) {
        return readStoredImage(Paths.get(storedPath));
    }

    public byte[] readStoredImage(Path storedPath) {
        try {
            return Files.readAllBytes(storedPath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("업로드된 이미지를 찾을 수 없습니다.");
        }
    }

    /**
     * 사용자 이미지 조회용 경로를 안전하게 해석합니다.
     * normalize 후 basePath 하위·user 디렉터리 하위인지 검증해 path traversal을 차단합니다.
     */
    public Path resolveSecureUserImagePath(String subdirectory, Long userId, String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        String decodedFilename;
        try {
            decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }
        if (decodedFilename.contains("\0")) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        Path baseDir = Paths.get(storageProperties.getLocal().getBasePath()).toAbsolutePath().normalize();
        Path userDir = baseDir.resolve(subdirectory).resolve(String.valueOf(userId)).normalize();
        if (!userDir.startsWith(baseDir)) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        Path resolved = userDir.resolve(decodedFilename).normalize();
        if (!resolved.startsWith(userDir)) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }
        return resolved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일을 선택해 주세요.");
        }

        if (file.getSize() > storageProperties.getClothes().getMaxSize()) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB 이하여야 합니다.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !storageProperties.getClothes().getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!storageProperties.getClothes().getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.");
        }

        validateImageMagicBytes(file);
    }

    // 클라이언트가 Content-Type·확장자를 위조해도 실제 파일 시그니처로 재검증
    private void validateImageMagicBytes(MultipartFile file) {
        byte[] header = new byte[IMAGE_HEADER_SIZE];
        int read;
        try (InputStream is = file.getInputStream()) {
            read = is.read(header, 0, IMAGE_HEADER_SIZE);
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
        }
        if (read < 3 || !isAllowedImageHeader(header, read)) {
            throw new IllegalArgumentException("실제 이미지 파일이 아닙니다. jpg, png, webp 형식만 업로드 가능합니다.");
        }
    }

    private boolean isAllowedImageHeader(byte[] header, int length) {
        // JPEG: FF D8 FF
        if (length >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (length >= 4
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return true;
        }
        // WebP: "RIFF" (52 49 46 46) at 0–3, "WEBP" (57 45 42 50) at 8–11
        if (length >= 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return true;
        }
        return false;
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredImage(
            String publicUrl,
            String storedPath,
            String contentType,
            String originalFilename
    ) {
    }
}
