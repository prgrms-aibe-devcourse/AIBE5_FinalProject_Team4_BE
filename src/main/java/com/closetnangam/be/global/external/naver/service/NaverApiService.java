package com.closetnangam.be.global.external.naver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class NaverApiService {

    // ⚠️ 중요: yml 경로(spring.security.oauth2.client.registration.naver...)와 완벽하게 싱크를 맞춥니다!
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    // 1. 메서드 이름을 쇼핑 검색에 맞게 수정했습니다.
    public String searchShop(String keyword) {
        RestTemplate restTemplate = new RestTemplate();

        // 2. path를 블로그(.blog)에서 쇼핑(.shop)으로 변경했습니다!
        URI targetUri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/shop.json")
                .queryParam("query", keyword)
                .queryParam("display", 10)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        // 2. 네이버 API 필수 헤더 설정 (아주 정확하게 잘 짜셨습니다!)
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. API 호출 및 응답 받기
        ResponseEntity<String> response = restTemplate.exchange(
                targetUri,
                HttpMethod.GET,
                entity,
                String.class
        );

        // 4. 결과 반환
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("네이버 쇼핑 API 호출 실패: " + response.getStatusCode());
        }
    }
}