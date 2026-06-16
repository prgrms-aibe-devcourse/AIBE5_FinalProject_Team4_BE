package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "s3")
public class S3ImageStorageService implements ImageStorageService {

    private static final String CLOTHES_PREFIX = "clothes";
    private static final String PURCHASE_CAPTURES_PREFIX = "purchase-captures";

    private final StorageProperties storageProperties;
    private final S3Client s3Client;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String awsRegion;

    @Value("${app.api.base-url:}")
    private String appBaseUrl;

    @PostConstruct
    void validateConfig() {
        if (!StringUtils.hasText(storageProperties.getS3().getBucket())) {
            throw new IllegalStateException("app.storage.backend=s3 일 때 app.storage.s3.bucket 설정이 필요합니다.");
        }
    }

    @Override
    public StoredImage storeClothesPhoto(Long userId, MultipartFile file) {
        ImageUploadValidator.validateFile(file, storageProperties);
        String extension = ImageUploadValidator.extractExtension(file.getOriginalFilename(), storageProperties);
        String objectKey = CLOTHES_PREFIX + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
        return uploadMultipart(objectKey, file, file.getContentType(), file.getOriginalFilename());
    }

    @Override
    public StoredImage storePurchaseCapture(Long userId, MultipartFile file) {
        ImageUploadValidator.validateFile(file, storageProperties);
        String extension = ImageUploadValidator.extractExtension(file.getOriginalFilename(), storageProperties);
        String objectKey = PURCHASE_CAPTURES_PREFIX + "/" + userId + "/" + UUID.randomUUID() + "." + extension;
        return uploadMultipart(objectKey, file, file.getContentType(), file.getOriginalFilename());
    }

    @Override
    public StoredImage storePurchaseCaptureThumbnail(Long userId, Long captureId, int itemIndex, byte[] jpegBytes) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IllegalArgumentException("업로드할 이미지 데이터가 없습니다.");
        }
        String fileName = captureId + "-item-" + itemIndex + ".jpg";
        String objectKey = PURCHASE_CAPTURES_PREFIX + "/" + userId + "/" + fileName;
        return uploadBytes(objectKey, jpegBytes, "image/jpeg", fileName);
    }

    @Override
    public byte[] readStoredImage(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            throw new IllegalArgumentException("업로드된 이미지를 찾을 수 없습니다.");
        }
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(storageProperties.getS3().getBucket())
                            .key(storedPath)
                            .build())
                    .asByteArray();
        } catch (S3Exception exception) {
            throw new IllegalArgumentException("업로드된 이미지를 찾을 수 없습니다.");
        }
    }

    @Override
    public byte[] readStoredImage(Path storedPath) {
        return readStoredImage(storedPath.toString());
    }

    @Override
    public String resolveUserImageStoredPath(String subdirectory, Long userId, String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        String decodedFilename;
        try {
            decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        // S3 object key는 사용자별 prefix 아래의 단일 파일명만 허용합니다.
        // 경로 탐색 문자열을 차단해 다른 사용자의 object key를 조합할 수 없게 합니다.
        if (!StringUtils.hasText(decodedFilename)
                || decodedFilename.contains("\0")
                || decodedFilename.contains("/")
                || decodedFilename.contains("\\")
                || decodedFilename.equals(".")
                || decodedFilename.equals("..")) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }

        return subdirectory + "/" + userId + "/" + decodedFilename;
    }

    @Override
    public Path resolveSecureUserImagePath(String subdirectory, Long userId, String filename) {
        throw new UnsupportedOperationException("S3 저장소는 로컬 이미지 경로 조회를 지원하지 않습니다.");
    }

    private StoredImage uploadMultipart(String objectKey, MultipartFile file, String contentType, String originalFilename) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredImage(buildPublicUrl(objectKey), objectKey, contentType, originalFilename);
        } catch (IOException | S3Exception exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private StoredImage uploadBytes(String objectKey, byte[] bytes, String contentType, String originalFilename) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return new StoredImage(buildPublicUrl(objectKey), objectKey, contentType, originalFilename);
        } catch (S3Exception exception) {
            throw new IllegalStateException("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String buildPublicUrl(String objectKey) {
        String publicBaseUrl = storageProperties.getS3().getPublicBaseUrl();
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + objectKey;
        }
        if (StringUtils.hasText(appBaseUrl)) {
            return appBaseUrl.replaceAll("/+$", "") + "/api/v1/images/" + objectKey;
        }
        String bucket = storageProperties.getS3().getBucket();
        return "https://" + bucket + ".s3." + awsRegion + ".amazonaws.com/" + objectKey;
    }
}
