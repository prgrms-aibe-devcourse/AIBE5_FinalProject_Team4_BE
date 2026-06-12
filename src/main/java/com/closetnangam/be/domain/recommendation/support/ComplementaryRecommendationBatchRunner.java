package com.closetnangam.be.domain.recommendation.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RECO-004 Gemini Batch API 오프라인 분류 실행기.
 *
 * <p>mode:
 * <ul>
 *   <li>{@code submit} — 네이버 수집 + JSONL 업로드 + batch job 생성 (state.json 저장)</li>
 *   <li>{@code import} — state.json의 job 결과 다운로드 + DB 적재</li>
 *   <li>{@code submit-and-wait} — submit 후 완료까지 폴링 + import</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.recommendation.complementary.batch", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ComplementaryRecommendationBatchRunner {

    private final ComplementaryRecommendationBatchService batchService;
    private final ObjectProvider<TaskExecutor> taskExecutorProvider;

    @Value("${app.recommendation.complementary.batch.mode:submit}")
    private String mode;

    @Value("${app.recommendation.complementary.batch.model:gemini-3.5-flash}")
    private String batchModel;

    @Value("${app.recommendation.complementary.batch.max-products:200}")
    private int maxProducts;

    @Value("${app.recommendation.complementary.batch.collection-mode:random}")
    private String collectionMode;

    @Value("${app.recommendation.complementary.batch.gap-fill-per-bucket:200}")
    private int gapFillPerBucket;

    @Value("${app.recommendation.complementary.batch.job-name:}")
    private String configuredJobName;

    @Value("${app.recommendation.complementary.batch.poll-interval-seconds:30}")
    private int pollIntervalSeconds;

    @Value("${app.recommendation.complementary.batch.max-poll-attempts:2880}")
    private int maxPollAttempts;

    @EventListener(ApplicationReadyEvent.class)
    public void runBatchJob() {
        log.info("[RECO-004 Batch] 백그라운드 실행 시작. mode={}, collectionMode={}, model={}, maxProducts={}, gapFillPerBucket={}",
                mode, collectionMode, batchModel, maxProducts, gapFillPerBucket);
        resolveTaskExecutor().execute(this::runSafely);
    }

    private void runSafely() {
        try {
            switch (normalizeMode(mode)) {
                case "submit" -> batchService.submitBatch(batchModel, maxProducts);
                case "import" -> batchService.importBatchResults(
                        resolveJobName(),
                        false,
                        pollIntervalSeconds,
                        maxPollAttempts
                );
                case "submit-and-wait" -> {
                    ComplementaryRecommendationBatchState state = batchService.submitBatch(batchModel, maxProducts);
                    batchService.importBatchResults(
                            state.jobName(),
                            true,
                            pollIntervalSeconds,
                            maxPollAttempts
                    );
                }
                default -> throw new IllegalArgumentException("지원하지 않는 batch mode 입니다: " + mode);
            }
        } catch (Exception exception) {
            log.error("[RECO-004 Batch] 실행 실패. mode={}, reason={}", mode, exception.getMessage(), exception);
        }
    }

    private String resolveJobName() {
        if (StringUtils.hasText(configuredJobName)) {
            return configuredJobName.trim();
        }
        return batchService.loadState()
                .map(ComplementaryRecommendationBatchState::jobName)
                .orElseThrow(() -> new IllegalStateException("batch.job-name 또는 state.json이 필요합니다."));
    }

    private TaskExecutor resolveTaskExecutor() {
        TaskExecutor taskExecutor = taskExecutorProvider.getIfAvailable();
        if (taskExecutor != null) {
            return taskExecutor;
        }
        SimpleAsyncTaskExecutor fallback = new SimpleAsyncTaskExecutor("reco-004-batch-");
        fallback.setDaemon(true);
        return fallback;
    }

    private static String normalizeMode(String value) {
        return value == null ? "submit" : value.trim().toLowerCase();
    }
}
