package com.closetnangam.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(@Value("${cloud.aws.region.static:ap-northeast-2}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
