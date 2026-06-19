package com.closetnangam.be.global.storage;

import com.closetnangam.be.global.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private StorageProperties storageProperties;
    private S3ImageStorageService s3ImageStorageService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.getS3().setBucket("closetnangam-images");
        storageProperties.getS3().setPublicBaseUrl("https://cdn.example.com");

        s3ImageStorageService = new S3ImageStorageService(storageProperties, s3Client);
        ReflectionTestUtils.setField(s3ImageStorageService, "awsRegion", "ap-northeast-2");
        s3ImageStorageService.validateConfig();
    }

    @Test
    void storeClothesPhoto_uploadsToS3AndReturnsPublicUrl() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        StoredImage storedImage = s3ImageStorageService.storeClothesPhoto(7L, file);

        assertThat(storedImage.publicUrl()).startsWith("https://cdn.example.com/clothes/7/");
        assertThat(storedImage.storedPath()).startsWith("clothes/7/");
        assertThat(storedImage.contentType()).isEqualTo("image/jpeg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("closetnangam-images");
        assertThat(requestCaptor.getValue().key()).startsWith("clothes/7/");
    }

    @Test
    void storePurchaseCaptureThumbnail_usesDeterministicObjectKey() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        StoredImage storedImage = s3ImageStorageService.storePurchaseCaptureThumbnail(
                3L,
                99L,
                1,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThat(storedImage.publicUrl())
                .isEqualTo("https://cdn.example.com/purchase-captures/3/99-item-1.jpg");
        assertThat(storedImage.storedPath()).isEqualTo("purchase-captures/3/99-item-1.jpg");

        verify(s3Client).putObject(
                eq(PutObjectRequest.builder()
                        .bucket("closetnangam-images")
                        .key("purchase-captures/3/99-item-1.jpg")
                        .contentType("image/jpeg")
                        .build()),
                any(RequestBody.class)
        );
    }
}
