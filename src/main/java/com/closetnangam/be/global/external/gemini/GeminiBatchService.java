package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.global.common.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gemini Batch API (JSONL 업로드 → batchGenerateContent → 결과 파일 다운로드).
 * RECO-004 대량 분류 오프라인 작업용.
 */
@Slf4j
@Service
public class GeminiBatchService {

    private static final String UPLOAD_BASE_URL = "https://generativelanguage.googleapis.com/upload/v1beta";
    private static final String DOWNLOAD_BASE_URL = "https://generativelanguage.googleapis.com/download/v1beta";
    private static final List<String> TERMINAL_STATES = List.of(
            "JOB_STATE_SUCCEEDED",
            "BATCH_STATE_SUCCEEDED",
            "JOB_STATE_FAILED",
            "BATCH_STATE_FAILED",
            "JOB_STATE_CANCELLED",
            "BATCH_STATE_CANCELLED",
            "JOB_STATE_EXPIRED",
            "BATCH_STATE_EXPIRED"
    );
    private static final String FILE_STATE_ACTIVE = "ACTIVE";
    private static final String FILE_STATE_FAILED = "FAILED";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiBaseUrl;
    private final int fileActivePollIntervalSeconds;
    private final int fileActiveMaxPollAttempts;

    public GeminiBatchService(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String apiBaseUrl,
            @Value("${app.recommendation.complementary.batch.file-active-poll-interval-seconds:5}") int fileActivePollIntervalSeconds,
            @Value("${app.recommendation.complementary.batch.file-active-max-poll-attempts:60}") int fileActiveMaxPollAttempts
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMinutes(5))
                .build();
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
        this.fileActivePollIntervalSeconds = fileActivePollIntervalSeconds;
        this.fileActiveMaxPollAttempts = fileActiveMaxPollAttempts;
    }

    /**
     * JSONL 업로드만 수행합니다. Gemini Files API ACTIVE 대기는 {@link #waitForFileActive(String)}에서 별도 처리하세요.
     * HTTP 요청·{@code @Transactional} 스레드에서 {@link #waitForFileActive} 장시간 폴링을 호출하지 마세요.
     */
    public String uploadJsonl(byte[] jsonlBytes, String displayName) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Gemini API 키가 설정되어 있지 않습니다.");
        }
        if (jsonlBytes == null || jsonlBytes.length == 0) {
            throw new IllegalArgumentException("Batch JSONL이 비어 있습니다.");
        }

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.set("x-goog-api-key", apiKey);
        startHeaders.set("X-Goog-Upload-Protocol", "resumable");
        startHeaders.set("X-Goog-Upload-Command", "start");
        startHeaders.set("X-Goog-Upload-Header-Content-Length", String.valueOf(jsonlBytes.length));
        startHeaders.set("X-Goog-Upload-Header-Content-Type", "application/jsonl");
        startHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> startBody = Map.of(
                "file", Map.of("display_name", displayName)
        );

        ResponseEntity<Void> startResponse;
        try {
            startResponse = restTemplate.exchange(
                    UPLOAD_BASE_URL + "/files",
                    HttpMethod.POST,
                    new HttpEntity<>(startBody, startHeaders),
                    Void.class
            );
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch JSONL 업로드 시작에 실패했습니다.", exception);
        }

        String uploadUrl = startResponse.getHeaders().getFirst("X-Goog-Upload-Url");
        if (!StringUtils.hasText(uploadUrl)) {
            throw new ExternalApiException("Gemini Batch 업로드 URL을 받지 못했습니다.");
        }

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.set("Content-Length", String.valueOf(jsonlBytes.length));
        uploadHeaders.set("X-Goog-Upload-Offset", "0");
        uploadHeaders.set("X-Goog-Upload-Command", "upload, finalize");

        ResponseEntity<String> uploadResponse;
        try {
            uploadResponse = restTemplate.exchange(
                    URI.create(uploadUrl),
                    HttpMethod.POST,
                    new HttpEntity<>(jsonlBytes, uploadHeaders),
                    String.class
            );
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch JSONL 업로드에 실패했습니다.", exception);
        }

        try {
            JsonNode root = objectMapper.readTree(uploadResponse.getBody());
            String fileName = root.path("file").path("name").asText(null);
            if (!StringUtils.hasText(fileName)) {
                throw new ExternalApiException("Gemini Batch 업로드 응답에 file.name이 없습니다.");
            }
            log.info("Gemini Batch JSONL 업로드 완료. file={}, bytes={}", fileName, jsonlBytes.length);
            return fileName;
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalApiException("Gemini Batch 업로드 응답을 해석하지 못했습니다.", exception);
        }
    }

    /**
     * JSONL 업로드 후 Files API ACTIVE까지 대기합니다. Batch submit 전용 — {@link #uploadJsonl}과 {@link #waitForFileActive}를
     * 각각 호출하지 마세요.
     */
    public String uploadJsonlAndWaitForActive(byte[] jsonlBytes, String displayName) {
        String fileName = uploadJsonl(jsonlBytes, displayName);
        waitForFileActive(fileName);
        return fileName;
    }

    public void waitForFileActive(String fileName) {
        waitForFileActive(fileName, fileActivePollIntervalSeconds, fileActiveMaxPollAttempts);
    }

    /**
     * Batch job 생성 전 input file ACTIVE 대기. 최대 {@code pollIntervalSeconds * maxPollAttempts}초 블로킹합니다.
     * 오프라인 배치 전용 — 웹 요청·DB 트랜잭션 스레드에서 호출하지 마세요.
     */
    public void waitForFileActive(String fileName, int pollIntervalSeconds, int maxPollAttempts) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("Batch input file name이 비어 있습니다.");
        }

        for (int attempt = 1; attempt <= maxPollAttempts; attempt++) {
            String state = getFileState(fileName);
            if (FILE_STATE_ACTIVE.equals(state)) {
                if (attempt > 1) {
                    log.info("Gemini Batch input file 준비 완료. file={}, attempts={}", fileName, attempt);
                }
                return;
            }
            if (FILE_STATE_FAILED.equals(state)) {
                throw new ExternalApiException("Gemini Batch input file 처리에 실패했습니다. file=" + fileName);
            }

            log.info("Gemini Batch input file 처리 대기. file={}, state={}, attempt={}/{}",
                    fileName, state, attempt, maxPollAttempts);
            sleepQuietly(pollIntervalSeconds * 1000L);
        }

        throw new ExternalApiException(
                "Gemini Batch input file이 ACTIVE 상태가 되지 않았습니다. file=" + fileName
                        + ", timeoutSeconds=" + (pollIntervalSeconds * maxPollAttempts)
        );
    }

    public String getFileState(String fileName) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/" + fileName,
                    HttpMethod.GET,
                    buildJsonEntity(null),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("state").asText(null);
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch input file 상태 조회에 실패했습니다. file=" + fileName, exception);
        } catch (Exception exception) {
            throw new ExternalApiException("Gemini Batch input file 응답을 해석하지 못했습니다. file=" + fileName, exception);
        }
    }

    public String createBatchJob(String model, String inputFileName, String displayName) {
        String url = apiBaseUrl + "/models/" + model + ":batchGenerateContent";
        Map<String, Object> body = Map.of(
                "batch", Map.of(
                        "display_name", displayName,
                        "input_config", Map.of("file_name", inputFileName)
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    buildJsonEntity(body),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            String jobName = root.path("name").asText(null);
            if (!StringUtils.hasText(jobName)) {
                throw new ExternalApiException("Gemini Batch job name이 없습니다.");
            }
            log.info("Gemini Batch job 생성. job={}, model={}, inputFile={}", jobName, model, inputFileName);
            return jobName;
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new ExternalApiException(
                    "Gemini Batch job 생성에 실패했습니다. status="
                            + exception.getStatusCode().value()
                            + ", body=" + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch job 생성에 실패했습니다.", exception);
        } catch (Exception exception) {
            throw new ExternalApiException("Gemini Batch job 응답을 해석하지 못했습니다.", exception);
        }
    }

    public GeminiBatchJobStatus getBatchJob(String jobName) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/" + jobName,
                    HttpMethod.GET,
                    buildJsonEntity(null),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            String state = firstNonBlank(
                    root.path("metadata").path("state").asText(null),
                    root.path("state").asText(null)
            );
            String responsesFile = firstNonBlank(
                    firstNonBlank(
                            root.path("dest").path("file_name").asText(null),
                            root.path("response").path("responsesFile").asText(null)
                    ),
                    root.path("metadata").path("output").path("responsesFile").asText(null)
            );
            JsonNode errorNode = root.path("error");
            if (errorNode.isMissingNode() || errorNode.isNull()) {
                errorNode = root.path("metadata").path("error");
            }
            return new GeminiBatchJobStatus(
                    jobName,
                    state,
                    responsesFile,
                    errorNode.isMissingNode() || errorNode.isNull() ? null : errorNode.toString(),
                    TERMINAL_STATES.contains(state)
            );
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch job 조회에 실패했습니다. job=" + jobName, exception);
        } catch (Exception exception) {
            throw new ExternalApiException("Gemini Batch job 응답을 해석하지 못했습니다.", exception);
        }
    }

    public String downloadResponsesFile(String responsesFile) {
        String url = DOWNLOAD_BASE_URL + "/" + responsesFile + ":download?alt=media";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildJsonEntity(null),
                    String.class
            );
            return response.getBody();
        } catch (RestClientException exception) {
            throw new ExternalApiException("Gemini Batch 결과 파일 다운로드에 실패했습니다.", exception);
        }
    }

    public String extractResponseText(JsonNode responseNode) {
        if (responseNode == null || responseNode.isMissingNode() || responseNode.isNull()) {
            return null;
        }
        JsonNode textNode = responseNode.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isMissingNode() && StringUtils.hasText(textNode.asText())) {
            return textNode.asText();
        }
        return null;
    }

    private HttpEntity<?> buildJsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-goog-api-key", apiKey);
        if (body == null) {
            return new HttpEntity<>(headers);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(second) ? second : null;
    }

    private static void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Gemini Batch input file 대기가 중단되었습니다.", exception);
        }
    }

    public record GeminiBatchJobStatus(
            String jobName,
            String state,
            String responsesFile,
            String error,
            boolean terminal
    ) {
        public boolean succeeded() {
            return "JOB_STATE_SUCCEEDED".equals(state) || "BATCH_STATE_SUCCEEDED".equals(state);
        }

        public boolean failed() {
            return "JOB_STATE_FAILED".equals(state)
                    || "BATCH_STATE_FAILED".equals(state)
                    || "JOB_STATE_CANCELLED".equals(state)
                    || "BATCH_STATE_CANCELLED".equals(state)
                    || "JOB_STATE_EXPIRED".equals(state)
                    || "BATCH_STATE_EXPIRED".equals(state);
        }
    }
}
