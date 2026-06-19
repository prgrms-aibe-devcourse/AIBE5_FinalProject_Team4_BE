package com.closetnangam.be.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** {@code local} 또는 {@code s3}. S3 img_url 연동 전까지는 {@code local}. */
    private String backend = "local";
    private Local local = new Local();
    private S3 s3 = new S3();
    private ClothesUpload clothes = new ClothesUpload();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "uploads";
        private String baseUrl = "http://localhost:8080/api/v1/images";
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket = "";
        /** CloudFront 또는 S3 public URL prefix. img_url 저장 시 사용 예정. */
        private String publicBaseUrl = "";
    }

    @Getter
    @Setter
    public static class ClothesUpload {
        private long maxSize = 10 * 1024 * 1024;
        private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
        private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
    }
}
