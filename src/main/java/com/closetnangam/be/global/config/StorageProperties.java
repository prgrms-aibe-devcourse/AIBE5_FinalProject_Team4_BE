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

    private Local local = new Local();
    private ClothesUpload clothes = new ClothesUpload();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "uploads";
        private String baseUrl = "http://localhost:8080/uploads";
    }

    @Getter
    @Setter
    public static class ClothesUpload {
        private long maxSize = 10 * 1024 * 1024;
        private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
        private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
    }
}
