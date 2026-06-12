package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.config.StorageProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

final class ImageUploadValidator {

    private static final int IMAGE_HEADER_SIZE = 12;

    private ImageUploadValidator() {
    }

    static void validateFile(MultipartFile file, StorageProperties storageProperties) {
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

        String extension = extractExtension(file.getOriginalFilename(), storageProperties);
        validateImageMagicBytes(file);
    }

    static String extractExtension(String originalFilename, StorageProperties storageProperties) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!storageProperties.getClothes().getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.");
        }
        return extension;
    }

    private static void validateImageMagicBytes(MultipartFile file) {
        byte[] header = new byte[IMAGE_HEADER_SIZE];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header, 0, IMAGE_HEADER_SIZE);
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
        }
        if (read < 3 || !isAllowedImageHeader(header, read)) {
            throw new IllegalArgumentException("실제 이미지 파일이 아닙니다. jpg, png, webp 형식만 업로드 가능합니다.");
        }
    }

    private static boolean isAllowedImageHeader(byte[] header, int length) {
        if (length >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF) {
            return true;
        }
        if (length >= 4
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return true;
        }
        if (length >= 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return true;
        }
        return false;
    }
}
