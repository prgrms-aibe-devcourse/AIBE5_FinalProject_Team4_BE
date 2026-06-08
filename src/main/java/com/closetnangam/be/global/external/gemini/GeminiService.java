package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
import com.closetnangam.be.global.common.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

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
            @Value("${gemini.model:gemini-3.5-flash}") String model
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                /*
                 * Gemini 3.5 계열은 짧은 JSON 응답에도 내부 thinking 시간이 포함될 수 있다.
                 * AI MD 추천처럼 후보 상품/옷장 컨텍스트를 함께 보내는 요청은 30초를 넘길 수 있어,
                 * 네트워크 연결 실패는 빠르게 감지하되 응답 대기 시간은 추천 기능 기준으로 여유를 둔다.
                 */
                .readTimeout(Duration.ofSeconds(90))
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

    public <T> T generateJsonFromText(String prompt, Class<T> resultType) {
        return generateJsonFromParts(List.of(Map.of("text", prompt)), resultType);
    }

    private <T> T generateJsonFromImage(byte[] imageBytes, String mimeType, String prompt, Class<T> resultType) {
        return generateJsonFromParts(
                List.of(
                        Map.of("text", prompt),
                        Map.of("inline_data", Map.of(
                                "mime_type", mimeType,
                                "data", Base64.getEncoder().encodeToString(imageBytes)
                        ))
                ),
                resultType
        );
    }

    private <T> T generateJsonFromParts(List<Map<String, Object>> parts, Class<T> resultType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 설정되어 있지 않습니다.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
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
            throw new ExternalApiException(resolveApiFailureMessage(exception), exception);
        }
    }

    private String resolveApiFailureMessage(RestClientException exception) {
        if (exception instanceof HttpStatusCodeException httpException) {
            int status = httpException.getStatusCode().value();
            String body = httpException.getResponseBodyAsString();
            log.warn("Gemini API call failed: status={}, model={}", status, model);
            if (status == 429 || (body != null && body.contains("RESOURCE_EXHAUSTED"))) {
                return "AI API 사용 한도를 초과했습니다. 잠시 후 다시 시도하거나 Google AI Studio 요금제·할당량을 확인해 주세요.";
            }
            if (status == 401 || status == 403) {
                return "Gemini API 키가 유효하지 않습니다. .env의 GEMINI_API_KEY를 확인해 주세요.";
            }
            if (status == 404) {
                return "Gemini 모델을 찾을 수 없습니다. application.yml의 gemini.model 설정을 확인해 주세요.";
            }
        } else {
            log.warn("Gemini API call failed: {}", exception.getMessage());
        }
        return "AI API 호출에 실패했습니다.";
    }

    private <T> T parseJsonResult(String responseBody, Class<T> resultType) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new ExternalApiException("AI 응답을 해석하지 못했습니다.", exception);
        }

        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new ExternalApiException("AI 응답을 해석할 수 없습니다.");
        }

        try {
            return objectMapper.readValue(textNode.asText(), resultType);
        } catch (Exception exception) {
            throw new ExternalApiException("AI 응답을 해석하지 못했습니다.", exception);
        }
    }
}
