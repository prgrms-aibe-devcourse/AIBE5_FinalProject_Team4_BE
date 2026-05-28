package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GeminiService(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.model:gemini-2.0-flash}") String model
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public GeminiClothingClassificationResult classifyClothingImage(
            byte[] imageBytes,
            String mimeType,
            String classificationGuide
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 설정되어 있지 않습니다.");
        }

        String prompt = """
                당신은 의류 분류 AI입니다. 아래 가이드의 코드만 사용해 JSON으로 응답하세요.
                name은 옷 이름(한국어), brandName은 브랜드명(모르면 "UNKNOWN")을 추정합니다.

                %s

                반드시 아래 JSON 형식만 반환하세요.
                {
                  "name": "string",
                  "brandName": "string",
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": ["NAVY"],
                  "styles": ["CASUAL"]
                }
                """.formatted(classificationGuide);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", Base64.getEncoder().encodeToString(imageBytes)
                                ))
                        ))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        // API 키는 URL이 아닌 헤더로 전달 (로그 유출 방지)
        String url = baseUrl + "/models/" + model + ":generateContent";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseClassificationResult(response.getBody());
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI 의류 판별 API 호출에 실패했습니다.");
        }
    }

    private GeminiClothingClassificationResult parseClassificationResult(String responseBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 판별 결과를 해석하지 못했습니다.");
        }

        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IllegalStateException("AI 응답을 해석할 수 없습니다.");
        }

        try {
            return objectMapper.readValue(textNode.asText(), GeminiClothingClassificationResult.class);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 판별 결과를 해석하지 못했습니다.");
        }
    }
}
