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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_ATTEMPTS_PER_MODEL = 2;
    private static final long RETRY_DELAY_MS = 1_000L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final List<String> modelsToTry;

    public GeminiService(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.fallback-models:}") List<String> fallbackModels
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.modelsToTry = buildModelsToTry(model, fallbackModels);
        log.info("Gemini API models: {}", modelsToTry);
    }

    private static List<String> buildModelsToTry(String primaryModel, List<String> fallbackModels) {
        List<String> models = new ArrayList<>();
        if (StringUtils.hasText(primaryModel)) {
            models.add(primaryModel.trim());
        }
        if (fallbackModels != null) {
            for (String fallbackModel : fallbackModels) {
                if (!StringUtils.hasText(fallbackModel)) {
                    continue;
                }
                String normalized = fallbackModel.trim();
                if (!models.contains(normalized)) {
                    models.add(normalized);
                }
            }
        }
        return List.copyOf(models);
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
                imageUrl에는 캡처 화면에 실제로 보이는 http(s) 상품 썸네일 URL만 넣으세요. URL을 읽을 수 없으면 null.

                반품·환불·취소·회수 완료 등 보유 옷장에 넣으면 안 되는 주문 행은 items에 절대 포함하지 마세요.
                예: "반품 완료", "반품 신청", "환불 완료", "주문 취소", "교환 완료" 상태 행은 제외합니다.
                포함 대상은 "구매 확정", "배송 완료", "결제 완료" 등 실제 보유한 구매 상품만입니다.

                화면에 등록 가능한 구매 상품(구매 확정·배송 완료 등)이 2개 이상이면 반드시 아래 규칙을 지키세요.
                - items 배열만 사용하고, 최상위 flat 필드(name, brandName 등)는 모두 null로 두세요.
                - items 길이는 등록 가능한 구매 상품 개수와 정확히 같아야 합니다. 첫 번째 상품만 넣지 마세요.
                - 각 item에는 orderStatus(화면 상태 문구)와 thumbnailRegion을 포함하세요.
                - thumbnailRegion은 해당 주문 행 왼쪽 정사각형 썸네일 영역의 0~1000 정규화 좌표(ymin,xmin,ymax,xmax)입니다.

                등록 대상 구매 상품이 화면에 1개뿐일 때만 최상위 flat 필드를 사용할 수 있습니다.
                반품·취소만 있고 등록 가능한 구매 상품이 없으면 name을 null로 두고 items는 []로 반환하세요.

                %s

                단일 상품 예시:
                {
                  "name": "string",
                  "brandName": "string",
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": [],
                  "styles": ["CASUAL"],
                  "optionText": "M / 네이비",
                  "suggestedExternalSource": "MUSINSA",
                  "imageUrl": "https://cdn.example.com/product.jpg",
                  "orderStatus": "구매 확정"
                }

                복수 상품 예시 (반품 행 제외, flat 필드는 null):
                {
                  "name": null,
                  "brandName": null,
                  "items": [
                    {
                      "name": "상품A",
                      "brandName": "브랜드A",
                      "category": "TOP",
                      "itemType": "SHORT_SLEEVE",
                      "primaryColor": "WHITE",
                      "secondaryColors": [],
                      "styles": ["CASUAL"],
                      "optionText": "L / 1개",
                      "suggestedExternalSource": "MUSINSA",
                      "imageUrl": null,
                      "orderStatus": "구매 확정",
                      "thumbnailRegion": { "ymin": 120, "xmin": 24, "ymax": 220, "xmax": 124 }
                    },
                    {
                      "name": "상품B",
                      "brandName": "브랜드B",
                      "category": "BOTTOM",
                      "itemType": "SLACKS",
                      "primaryColor": "BLACK",
                      "secondaryColors": [],
                      "styles": ["CASUAL"],
                      "optionText": "36 / 1개",
                      "suggestedExternalSource": "MUSINSA",
                      "imageUrl": null,
                      "orderStatus": "구매 확정",
                      "thumbnailRegion": { "ymin": 320, "xmin": 24, "ymax": 420, "xmax": 124 }
                    }
                  ]
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        RestClientException lastException = null;
        for (String modelName : modelsToTry) {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    ResponseEntity<String> response = postGenerateContent(modelName, entity);
                    if (!modelName.equals(model)) {
                        log.info("Gemini API succeeded with fallback model: {}", modelName);
                    }
                    return parseJsonResult(response.getBody(), resultType);
                } catch (RestClientException exception) {
                    lastException = exception;
                    if (shouldRetry(exception, attempt)) {
                        log.warn(
                                "Gemini API transient failure: model={}, attempt={}/{}",
                                modelName,
                                attempt,
                                MAX_ATTEMPTS_PER_MODEL
                        );
                        sleepQuietly(RETRY_DELAY_MS);
                        continue;
                    }
                    break;
                }
            }
        }

        throw new IllegalStateException(resolveApiFailureMessage(lastException), lastException);
    }

    private ResponseEntity<String> postGenerateContent(String modelName, HttpEntity<Map<String, Object>> entity) {
        String url = baseUrl + "/models/" + modelName + ":generateContent";
        return restTemplate.postForEntity(url, entity, String.class);
    }

    private boolean shouldRetry(RestClientException exception, int attempt) {
        if (attempt >= MAX_ATTEMPTS_PER_MODEL) {
            return false;
        }
        if (exception instanceof HttpStatusCodeException httpException) {
            int status = httpException.getStatusCode().value();
            String body = httpException.getResponseBodyAsString();
            return status == 503
                    || status == 429
                    || status == 500
                    || (body != null && (body.contains("UNAVAILABLE") || body.contains("RESOURCE_EXHAUSTED")));
        }
        String message = exception.getMessage();
        return message != null
                && (message.contains("Read timed out") || message.contains("connect timed out"));
    }

    private void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveApiFailureMessage(RestClientException exception) {
        if (exception == null) {
            return "AI API 호출에 실패했습니다.";
        }
        if (exception instanceof HttpStatusCodeException httpException) {
            int status = httpException.getStatusCode().value();
            String body = httpException.getResponseBodyAsString();
            log.warn("Gemini API call failed: status={}, models={}", status, modelsToTry);
            if (status == 503 || (body != null && body.contains("UNAVAILABLE"))) {
                return "AI 서버가 일시적으로 과부하 상태입니다. 잠시 후 다시 시도해 주세요.";
            }
            if (status == 429 || (body != null && body.contains("RESOURCE_EXHAUSTED"))) {
                return "AI API 사용 한도를 초과했습니다. 잠시 후 다시 시도하거나 Google AI Studio 요금제·할당량을 확인해 주세요.";
            }
            if (status == 401 || status == 403) {
                return "Gemini API 키가 유효하지 않습니다. .env의 GEMINI_API_KEY를 확인해 주세요.";
            }
            if (status == 404) {
                return "Gemini 모델을 찾을 수 없습니다. application.yml의 gemini.model 설정을 확인해 주세요.";
            }
            if (status == 500) {
                return "AI 서버에서 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            }
        } else {
            log.warn("Gemini API call failed: {}", exception.getMessage());
            String message = exception.getMessage();
            if (message != null
                    && (message.contains("Read timed out") || message.contains("connect timed out"))) {
                return "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            }
        }
        return "AI API 호출에 실패했습니다.";
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
