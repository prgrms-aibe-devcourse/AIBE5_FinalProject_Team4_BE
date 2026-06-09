package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import com.closetnangam.be.global.external.gemini.GeminiBatchService;
import com.closetnangam.be.global.external.gemini.GeminiBatchService.GeminiBatchJobStatus;
import com.closetnangam.be.global.external.gemini.dto.GeminiClothingClassificationResult;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.closetnangam.be.global.external.naver.service.NaverApiService;
import com.closetnangam.be.global.external.naver.support.NaverShoppingImageDownloader;
import com.closetnangam.be.global.external.naver.support.NaverShoppingTitleSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RECO-004 Gemini Batch API 기반 대량 분류·적재.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplementaryRecommendationBatchService {

    private static final int NAVER_PAGE_SIZE = 100;
    private static final int MAX_COLLECT_ATTEMPTS = 300;
    private static final String NAVER_EXCLUDE = "used:rental:cbshop";
    private static final String PRICE_COMPARISON_PRODUCT_TYPE = "1";
    private static final List<String> SORT_OPTIONS = List.of("sim", "date", "asc", "dsc");
    private static final List<String> SEARCH_KEYWORDS = List.of(
            "남성 캐주얼 티셔츠",
            "여성 블라우스",
            "남성 맨투맨",
            "여성 니트",
            "남성 슬랙스",
            "여성 와이드 팬츠",
            "남성 데님 팬츠",
            "여성 미디 스커트",
            "남성 경량 패딩",
            "여성 트렌치 코트",
            "남성 바람막이",
            "여성 가디건",
            "화이트 스니커즈",
            "남성 로퍼",
            "여성 플랫슈즈",
            "남성 워커 부츠"
    );

    private final GeminiBatchService geminiBatchService;
    private final NaverApiService naverApiService;
    private final ClothesRepository clothesRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final ComplementaryRecommendationClassificationService classificationService;
    private final ExternalClothesService externalClothesService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.recommendation.complementary.batch.state-dir:build/reco-004-batch}")
    private String stateDir;

    public ComplementaryRecommendationBatchState submitBatch(String model, int maxProducts) {
        List<NaverShoppingProductResponse> products = collectProducts(maxProducts);
        if (products.isEmpty()) {
            throw new IllegalStateException("Batch에 넣을 신규 상품이 없습니다.");
        }

        JsonlBuildResult jsonlBuildResult = buildJsonl(products);
        if (jsonlBuildResult.includedProducts().isEmpty()) {
            throw new IllegalStateException("Batch JSONL에 포함할 이미지 다운로드 가능한 상품이 없습니다.");
        }

        byte[] jsonlBytes = jsonlBuildResult.jsonlBytes();
        String displayName = "reco-004-batch-" + Instant.now().toEpochMilli();
        String inputFileName = geminiBatchService.uploadJsonl(jsonlBytes, displayName);
        String jobName = geminiBatchService.createBatchJob(model, inputFileName, displayName);

        ComplementaryRecommendationBatchState state = new ComplementaryRecommendationBatchState(
                jobName,
                model,
                Instant.now(),
                jsonlBuildResult.includedProducts()
        );
        saveState(state);
        log.info("[RECO-004 Batch] 제출 완료. job={}, collected={}, submitted={}, imageSkipped={}",
                jobName,
                products.size(),
                jsonlBuildResult.includedProducts().size(),
                jsonlBuildResult.skippedImageCount());
        return state;
    }

    public BatchImportSummary importBatchResults(String jobName, boolean waitUntilComplete, int pollIntervalSeconds, int maxPollAttempts) {
        ComplementaryRecommendationBatchState state = loadState()
                .filter(saved -> jobName == null || jobName.equals(saved.jobName()))
                .orElseThrow(() -> new IllegalStateException("Batch state 파일을 찾을 수 없습니다. jobName=" + jobName));

        GeminiBatchJobStatus status = waitForCompletion(
                state.jobName(),
                waitUntilComplete,
                pollIntervalSeconds,
                maxPollAttempts
        );
        if (!status.succeeded()) {
            throw new IllegalStateException("Batch job이 성공하지 않았습니다. state=" + status.state() + ", error=" + status.error());
        }
        if (!StringUtils.hasText(status.responsesFile())) {
            throw new IllegalStateException("Batch 결과 파일 경로가 없습니다. job=" + state.jobName());
        }

        String responsesJsonl = geminiBatchService.downloadResponsesFile(status.responsesFile());
        Map<String, NaverShoppingProductResponse> productMap = new LinkedHashMap<>();
        for (NaverShoppingProductResponse product : state.products()) {
            if (StringUtils.hasText(product.productId())) {
                productMap.put(product.productId().trim(), product);
            }
        }

        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (String line : responsesJsonl.split("\\R")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(line);
                String productId = root.path("key").asText(null);
                if (!StringUtils.hasText(productId)) {
                    failed++;
                    continue;
                }
                NaverShoppingProductResponse product = productMap.get(productId);
                if (product == null) {
                    skipped++;
                    continue;
                }

                JsonNode responseNode = root.path("response");
                if (responseNode.isMissingNode() || responseNode.has("error")) {
                    failed++;
                    continue;
                }

                String responseText = geminiBatchService.extractResponseText(responseNode);
                if (!StringUtils.hasText(responseText)) {
                    failed++;
                    continue;
                }

                GeminiClothingClassificationResult result =
                        objectMapper.readValue(responseText, GeminiClothingClassificationResult.class);
                Optional<ComplementaryRecommendationClassificationService.ResolvedClassification> resolved =
                        classificationService.resolveBatchClassification(result);
                if (resolved.isEmpty()) {
                    skipped++;
                    continue;
                }

                Optional<Long> clothesId = externalClothesService.importClassifiedComplementaryCandidate(
                        product,
                        resolved.get()
                );
                if (clothesId.isPresent()) {
                    imported++;
                } else {
                    skipped++;
                }
            } catch (Exception exception) {
                failed++;
                log.warn("[RECO-004 Batch] 결과 라인 처리 실패. reason={}", exception.getMessage());
            }
        }

        BatchImportSummary summary = new BatchImportSummary(state.jobName(), imported, skipped, failed);
        log.info("[RECO-004 Batch] import 완료. job={}, imported={}, skipped={}, failed={}",
                summary.jobName(), summary.imported(), summary.skipped(), summary.failed());
        return summary;
    }

    private GeminiBatchJobStatus waitForCompletion(
            String jobName,
            boolean waitUntilComplete,
            int pollIntervalSeconds,
            int maxPollAttempts
    ) {
        GeminiBatchJobStatus status = geminiBatchService.getBatchJob(jobName);
        if (!waitUntilComplete || status.terminal()) {
            return status;
        }

        int attempts = 0;
        while (!status.terminal() && attempts < maxPollAttempts) {
            log.info("[RECO-004 Batch] job 상태. job={}, state={}, attempt={}/{}",
                    jobName, status.state(), attempts + 1, maxPollAttempts);
            sleepQuietly(pollIntervalSeconds * 1000L);
            status = geminiBatchService.getBatchJob(jobName);
            attempts++;
        }
        return status;
    }

    private List<NaverShoppingProductResponse> collectProducts(int maxProducts) {
        List<NaverShoppingProductResponse> collected = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < MAX_COLLECT_ATTEMPTS && collected.size() < maxProducts; attempt++) {
            String keyword = SEARCH_KEYWORDS.get(random.nextInt(SEARCH_KEYWORDS.size()));
            int start = 1 + random.nextInt(10) * NAVER_PAGE_SIZE;
            String sort = SORT_OPTIONS.get(random.nextInt(SORT_OPTIONS.size()));

            List<NaverShoppingProductResponse> products = naverApiService.searchShoppingProducts(
                    keyword,
                    NAVER_PAGE_SIZE,
                    start,
                    sort,
                    NAVER_EXCLUDE
            );
            for (NaverShoppingProductResponse product : products) {
                if (!StringUtils.hasText(product.productId())) {
                    continue;
                }
                if (!isPriceComparisonProduct(product)) {
                    continue;
                }
                String productId = product.productId().trim();
                if (clothesRepository.findByExternalProductId(productId).isPresent()) {
                    continue;
                }
                String cleanTitle = sanitizeTitle(product);
                if (!ComplementaryRecommendationProductFilter.isWearableCandidate(product, cleanTitle)) {
                    continue;
                }
                if (collected.stream().anyMatch(item -> productId.equals(item.productId()))) {
                    continue;
                }
                collected.add(product);
                if (collected.size() >= maxProducts) {
                    break;
                }
            }
        }

        if (collected.size() < maxProducts) {
            log.warn("[RECO-004 Batch] 가격비교 상품 수집 목표 미달. target={}, collected={}", maxProducts, collected.size());
        } else {
            log.info("[RECO-004 Batch] 가격비교 상품 수집 완료. count={}", collected.size());
        }
        return collected;
    }

    private static boolean isPriceComparisonProduct(NaverShoppingProductResponse product) {
        return product != null
                && StringUtils.hasText(product.productType())
                && PRICE_COMPARISON_PRODUCT_TYPE.equals(product.productType().trim());
    }

    private JsonlBuildResult buildJsonl(List<NaverShoppingProductResponse> products) {
        String guide = categoryCatalogService.getAiClassificationGuide();
        StringBuilder builder = new StringBuilder();
        List<NaverShoppingProductResponse> includedProducts = new ArrayList<>();
        int skippedImageCount = 0;

        for (NaverShoppingProductResponse product : products) {
            String cleanTitle = sanitizeTitle(product);
            Optional<NaverShoppingImageDownloader.DownloadedImage> imageOptional =
                    NaverShoppingImageDownloader.tryDownload(restTemplate, product.image());
            if (imageOptional.isEmpty()) {
                skippedImageCount++;
                log.warn("[RECO-004 Batch] 이미지 다운로드 실패 — Batch 제외. productId={}, imageUrl={}",
                        product.productId(), product.image());
                continue;
            }
            NaverShoppingImageDownloader.DownloadedImage image = imageOptional.get();
            String prompt = ComplementaryRecommendationGeminiPrompts.buildShoppingProductPrompt(
                    guide,
                    cleanTitle,
                    product.brand(),
                    product.category3()
            );

            Map<String, Object> request = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", image.mimeType(),
                                            "data", Base64.getEncoder().encodeToString(image.bytes())
                                    ))
                            ))
                    ),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );
            Map<String, Object> line = Map.of(
                    "key", product.productId().trim(),
                    "request", request
            );

            try {
                builder.append(objectMapper.writeValueAsString(line)).append('\n');
                includedProducts.add(product);
            } catch (Exception exception) {
                throw new IllegalStateException("Batch JSONL 생성에 실패했습니다. productId=" + product.productId(), exception);
            }
        }

        if (skippedImageCount > 0) {
            log.warn("[RECO-004 Batch] 이미지 실패로 Batch 제외된 상품. skipped={}", skippedImageCount);
        }
        return new JsonlBuildResult(
                builder.toString().getBytes(StandardCharsets.UTF_8),
                includedProducts,
                skippedImageCount
        );
    }

    private void saveState(ComplementaryRecommendationBatchState state) {
        try {
            Path dir = Path.of(stateDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("state.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), state);
            log.info("[RECO-004 Batch] state 저장. path={}", file.toAbsolutePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Batch state 저장에 실패했습니다.", exception);
        }
    }

    public Optional<ComplementaryRecommendationBatchState> loadState() {
        Path file = Path.of(stateDir).resolve("state.json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), ComplementaryRecommendationBatchState.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Batch state 로드에 실패했습니다. path=" + file, exception);
        }
    }

    private static String sanitizeTitle(NaverShoppingProductResponse product) {
        if (product == null) {
            return "";
        }
        return NaverShoppingTitleSanitizer.sanitize(product.title(), product.productId()).displayName();
    }

    private static void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record JsonlBuildResult(byte[] jsonlBytes, List<NaverShoppingProductResponse> includedProducts, int skippedImageCount) {
    }

    public record BatchImportSummary(String jobName, int imported, int skipped, int failed) {
    }
}
