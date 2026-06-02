package com.closetnangam.be.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * 외부 API 호출에 공통으로 사용할 RestTemplate.
     * 타임아웃이 없으면 네이버/날씨 같은 외부 API 지연이 애플리케이션 스레드를 오래 붙잡을 수 있다.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
