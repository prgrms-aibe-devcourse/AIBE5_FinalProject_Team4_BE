package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.domain.recommendation.support.ComplementaryRecommendationGeminiPrompts;
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
    private final List<String> apiKeysToTry;
    private final String baseUrl;
    private final String model;
    private final List<String> modelsToTry;

    public GeminiService(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.api-keys:}") List<String> configuredApiKeys,
            @Value("${GEMINI_API_KEY_2:}") String backupApiKey2,
            @Value("${GEMINI_API_KEY_3:}") String backupApiKey3,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.fallback-models:}") List<String> fallbackModels
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
        this.apiKeysToTry = buildApiKeysToTry(apiKey, configuredApiKeys, backupApiKey2, backupApiKey3);
        this.baseUrl = baseUrl;
        this.model = model;
        this.modelsToTry = buildModelsToTry(model, fallbackModels);
        log.info("Gemini API keys configured: {}", apiKeysToTry.size());
        log.info("Gemini API models: {}", modelsToTry);
    }

    /**
     * 키 우선순위: application.yml gemini.api-keys → gemini.api-key → GEMINI_API_KEY_2/3 환경 변수.
     * IntelliJ EnvFile은 OS env로 주입되므로 YAML 리스트 바인딩과 별도로 예비 키 env를 직접 읽는다.
     */
    private static List<String> buildApiKeysToTry(
            String primaryApiKey,
            List<String> configuredApiKeys,
            String backupApiKey2,
            String backupApiKey3
    ) {
        List<String> keys = new ArrayList<>();
        appendUniqueKey(keys, primaryApiKey);
        if (configuredApiKeys != null) {
            for (String configuredApiKey : configuredApiKeys) {
                appendUniqueKey(keys, configuredApiKey);
            }
        }
        appendUniqueKey(keys, backupApiKey2);
        appendUniqueKey(keys, backupApiKey3);
        return List.copyOf(keys);
    }

    private static void appendUniqueKey(List<String> keys, String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return;
        }
        String normalized = candidate.trim();
        if (!keys.contains(normalized)) {
            keys.add(normalized);
        }
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
                분류 순서: 1) category·itemType·색상·스타일 2) gender — 이미지 속 착용 모델의 성별을 우선 확인하고,
                모델이 없으면 옷 종류·상품명으로 추정하세요. 남녀 공용이면 UNISEX.
                3) season — 두께·소재·소매 길이·보온성으로 착용 시즌을 판별하세요. 불확실하면 ALL_SEASON.

                %s

                반드시 아래 JSON 형식만 반환하세요.
                {
                  "name": "string",
                  "brandName": "string",
                  "category": "TOP",
                  "itemType": "SHORT_SLEEVE",
                  "primaryColor": "WHITE",
                  "secondaryColors": ["NAVY"],
                  "styles": ["CASUAL"],
                  "gender": "UNISEX",
                  "season": "ALL_SEASON"
                }
                """.formatted(classificationGuide);

        return generateJsonFromImage(imageBytes, mimeType, prompt, GeminiClothingClassificationResult.class);
    }

    /**
     * 쇼핑몰 상품 이미지 + 텍스트 메타정보로 의류 분류.
     * RECO-004 외부 후보 시드 적재 시 사용한다.
     */
    public GeminiClothingClassificationResult classifyShoppingProduct(
            byte[] imageBytes,
            String mimeType,
            String classificationGuide,
            String productTitle,
            String brandName,
            String shoppingCategory
    ) {
        String prompt = ComplementaryRecommendationGeminiPrompts.buildShoppingProductPrompt(
                classificationGuide,
                productTitle,
                brandName,
                shoppingCategory
        );

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
                gender는 상품명·옵션·썸네일 모델로 추정하고, 불확실하면 UNISEX를 사용하세요.
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
                  "gender": "UNISEX",
                  "season": "ALL_SEASON",
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
                      "gender": "UNISEX",
                      "season": "ALL_SEASON",
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
                      "gender": "UNISEX",
                      "season": "ALL_SEASON",
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
        if (apiKeysToTry.isEmpty()) {
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

        RestClientException lastException = null;
        for (int keyIndex = 0; keyIndex < apiKeysToTry.size(); keyIndex++) {
            String apiKey = apiKeysToTry.get(keyIndex);
            HttpEntity<Map<String, Object>> entity = buildRequestEntity(requestBody, apiKey);
            boolean switchToNextApiKey = false;

            for (String modelName : modelsToTry) {
                for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODEL; attempt++) {
                    try {
                        log.info(
                                "Gemini API 요청 중: keyIndex={}, model={}, attempt={}/{}",
                                keyIndex + 1,
                                modelName,
                                attempt,
                                MAX_ATTEMPTS_PER_MODEL
                        );
                        ResponseEntity<String> response = postGenerateContent(modelName, entity);
                        if (keyIndex > 0) {
                            log.info("Gemini API succeeded with fallback key index {}", keyIndex + 1);
                        }
                        if (!modelName.equals(model)) {
                            log.info("Gemini API succeeded with fallback model: {}", modelName);
                        }
                        return parseJsonResult(response.getBody(), resultType);
                    } catch (RestClientException exception) {
                        lastException = exception;
                        if (shouldSwitchApiKey(exception, keyIndex)) {
                            log.warn(
                                    "Gemini API key unavailable, trying next key. keyIndex={}/{}, reason={}",
                                    keyIndex + 1,
                                    apiKeysToTry.size(),
                                    summarizeException(exception)
                            );
                            switchToNextApiKey = true;
                            break;
                        }
                        if (shouldRetryTransient(exception, attempt)) {
                            log.warn(
                                    "Gemini API transient failure: keyIndex={}, model={}, attempt={}/{}",
                                    keyIndex + 1,
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
                if (switchToNextApiKey) {
                    break;
                }
            }
        }

        throw new IllegalStateException(resolveApiFailureMessage(lastException, apiKeysToTry.size()), lastException);
    }

    private HttpEntity<Map<String, Object>> buildRequestEntity(Map<String, Object> requestBody, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        return new HttpEntity<>(requestBody, headers);
    }

    private ResponseEntity<String> postGenerateContent(String modelName, HttpEntity<Map<String, Object>> entity) {
        String url = baseUrl + "/models/" + modelName + ":generateContent";
        return restTemplate.postForEntity(url, entity, String.class);
    }

    private boolean shouldSwitchApiKey(RestClientException exception, int keyIndex) {
        if (keyIndex >= apiKeysToTry.size() - 1) {
            return false;
        }
        if (isQuotaExceeded(exception) || isInvalidApiKey(exception)) {
            return true;
        }
        return false;
    }

    private boolean isQuotaExceeded(RestClientException exception) {
        if (!(exception instanceof HttpStatusCodeException httpException)) {
            return false;
        }
        int status = httpException.getStatusCode().value();
        String body = httpException.getResponseBodyAsString();
        return status == 429 || (body != null && body.contains("RESOURCE_EXHAUSTED"));
    }

    private boolean isInvalidApiKey(RestClientException exception) {
        if (!(exception instanceof HttpStatusCodeException httpException)) {
            return false;
        }
        int status = httpException.getStatusCode().value();
        return status == 401 || status == 403;
    }

    private boolean shouldRetryTransient(RestClientException exception, int attempt) {
        if (attempt >= MAX_ATTEMPTS_PER_MODEL || isQuotaExceeded(exception) || isInvalidApiKey(exception)) {
            return false;
        }
        if (exception instanceof HttpStatusCodeException httpException) {
            int status = httpException.getStatusCode().value();
            String body = httpException.getResponseBodyAsString();
            return status == 503
                    || status == 500
                    || (body != null && body.contains("UNAVAILABLE"));
        }
        String message = exception.getMessage();
        return message != null
                && (message.contains("Read timed out") || message.contains("connect timed out"));
    }

    private String summarizeException(RestClientException exception) {
        if (exception instanceof HttpStatusCodeException httpException) {
            return "status=" + httpException.getStatusCode().value();
        }
        return exception.getMessage();
    }

    private void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveApiFailureMessage(RestClientException exception, int configuredApiKeyCount) {
        if (exception == null) {
            return "AI API 호출에 실패했습니다.";
        }
        if (exception instanceof HttpStatusCodeException httpException) {
            int status = httpException.getStatusCode().value();
            String body = httpException.getResponseBodyAsString();
            log.warn(
                    "Gemini API call failed: status={}, models={}, configuredApiKeys={}",
                    status,
                    modelsToTry,
                    configuredApiKeyCount
            );
            if (status == 503 || (body != null && body.contains("UNAVAILABLE"))) {
                return "AI 서버가 일시적으로 과부하 상태입니다. 잠시 후 다시 시도해 주세요.";
            }
            if (status == 429 || (body != null && body.contains("RESOURCE_EXHAUSTED"))) {
                if (configuredApiKeyCount > 1) {
                    return "모든 Gemini API 키의 사용 한도를 초과했습니다. 잠시 후 다시 시도하거나 예비 키·할당량을 확인해 주세요.";
                }
                return "AI API 사용 한도를 초과했습니다. 잠시 후 다시 시도하거나 Google AI Studio 요금제·할당량을 확인해 주세요.";
            }
            if (status == 401 || status == 403) {
                if (configuredApiKeyCount > 1) {
                    return "설정된 모든 Gemini API 키가 유효하지 않습니다. .env의 GEMINI_API_KEY 및 예비 키를 확인해 주세요.";
                }
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
