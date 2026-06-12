package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    private final StorageProperties storageProperties;

    @Override
    public StoredImage storeClothesPhoto(Long userId, MultipartFile file) {
        ImageUploadValidator.validateFile(file, storageProperties);

        String extension = ImageUploadValidator.extractExtension(file.getOriginalFilename(), storageProperties);
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

    @Override
    public StoredImage storePurchaseCapture(Long userId, MultipartFile file) {
        ImageUploadValidator.validateFile(file, storageProperties);

        String extension = ImageUploadValidator.extractExtension(file.getOriginalFilename(), storageProperties);
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

    @Override
    public StoredImage storePurchaseCaptureThumbnail(Long userId, Long captureId, int itemIndex, byte[] jpegBytes) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IllegalArgumentException("업로드할 이미지 데이터가 없습니다.");
        }

        String fileName = captureId + "-item-" + itemIndex + ".jpg";
        Path targetDirectory = Paths.get(
                storageProperties.getLocal().getBasePath(),
                "purchase-captures",
                String.valueOf(userId)
        );
        Path targetPath = targetDirectory.resolve(fileName);

        try {
            Files.createDirectories(targetDirectory);
            Files.write(targetPath, jpegBytes);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String publicUrl = storageProperties.getLocal().getBaseUrl()
                + "/purchase-captures/" + userId + "/" + fileName;

        return new StoredImage(publicUrl, targetPath.toString(), "image/jpeg", fileName);
    }

    @Override
    public byte[] readStoredImage(String storedPath) {
        return readStoredImage(Paths.get(storedPath));
    }

    @Override
    public byte[] readStoredImage(Path storedPath) {
        try {
            return Files.readAllBytes(storedPath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("업로드된 이미지를 찾을 수 없습니다.");
        }
    }

    @Override
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

    @Override
    public boolean supportsLocalImageServing() {
        return true;
    }
}
