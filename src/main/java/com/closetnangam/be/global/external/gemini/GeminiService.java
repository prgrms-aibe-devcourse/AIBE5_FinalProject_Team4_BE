package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
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

        return generateJsonFromImage(imageBytes, mimeType, prompt, GeminiClothingClassificationResult.class);
    }

    public GeminiPurchaseCaptureExtractionResult extractPurchaseCaptureInfo(
            byte[] imageBytes,
            String mimeType,
            String extractionGuide
    ) {
        String prompt = """
                당신은 쇼핑몰 구매내역 캡처 OCR AI입니다.
                이미지 속 텍스트를 읽어 상품 정보를 추출하고, 아래 가이드의 코드만 사용해 JSON으로 응답하세요.
                추출할 수 없는 필드는 null 또는 빈 배열 []을 사용하세요.
                brandName을 모르면 "UNKNOWN"을 사용하세요.
                optionText에는 사이즈·색상·옵션 등 주문 옵션 텍스트를 그대로 넣으세요.
                suggestedExternalSource는 화면/로고/URL로 추정 가능한 쇼핑몰 코드입니다. 불확실하면 null.

                %s

                반드시 아래 JSON 형식만 반환하세요.
                {
                  "name": "string",
                  "brandName": "string",
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": [],
                  "styles": ["CASUAL"],
                  "optionText": "M / 네이비",
                  "suggestedExternalSource": "MUSINSA"
                }
                """.formatted(extractionGuide);

        return generateJsonFromImage(imageBytes, mimeType, prompt, GeminiPurchaseCaptureExtractionResult.class);
    }

    private <T> T generateJsonFromImage(byte[] imageBytes, String mimeType, String prompt, Class<T> resultType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 설정되어 있지 않습니다.");
        }

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

        String url = baseUrl + "/models/" + model + ":generateContent";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseJsonResult(response.getBody(), resultType);
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI API 호출에 실패했습니다.");
        }
    }

    private <T> T parseJsonResult(String responseBody, Class<T> resultType) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 응답을 해석하지 못했습니다.");
        }

        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IllegalStateException("AI 응답을 해석할 수 없습니다.");
        }

        try {
            return objectMapper.readValue(textNode.asText(), resultType);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 응답을 해석하지 못했습니다.");
        }
    }
}
