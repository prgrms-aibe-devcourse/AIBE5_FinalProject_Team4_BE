package com.closetnangam.be.global.external.gemini;

import com.closetnangam.be.global.common.exception.ExternalApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceTest {

    @Test
    @DisplayName("예비 API 키 목록에서 빈 값과 중복을 제거한다")
    void buildApiKeysToTryFiltersBlankAndDuplicateKeys() {
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) ReflectionTestUtils.invokeMethod(
                GeminiService.class,
                "buildApiKeysToTry",
                "primary-key",
                List.of("primary-key", "", "  ", "backup-key", "backup-key"),
                "backup-key-2",
                ""
        );

        assertThat(keys).containsExactly("primary-key", "backup-key", "backup-key-2");
    }

    @Test
    @DisplayName("YAML 리스트가 비어 있어도 GEMINI_API_KEY_2/3 환경 변수를 사용한다")
    void buildApiKeysToTryUsesBackupEnvironmentVariables() {
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) ReflectionTestUtils.invokeMethod(
                GeminiService.class,
                "buildApiKeysToTry",
                "primary-key",
                List.of("primary-key"),
                "backup-key-2",
                "backup-key-3"
        );

        assertThat(keys).containsExactly("primary-key", "backup-key-2", "backup-key-3");
    }

    @Test
    @DisplayName("Gemini 200 응답 구조가 비어 있으면 외부 API 오류로 분류한다")
    void parseJsonResultThrowsExternalApiExceptionWhenCandidateTextIsMissing() {
        /*
         * Gemini가 HTTP 200을 반환해도 candidates/text가 없으면 upstream 응답 불량이다.
         * FE가 보유 옷 없음 같은 409 상태와 구분할 수 있도록 502로 매핑되는 ExternalApiException을 유지한다.
         */
        GeminiService geminiService = new GeminiService(
                new ObjectMapper(),
                new RestTemplateBuilder(),
                "test-api-key",
                List.of(),
                "",
                "",
                "https://example.com",
                "gemini-test",
                List.of()
        );
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": []
                      }
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                geminiService,
                "parseJsonResult",
                responseBody,
                DummyResult.class
        )).isInstanceOf(ExternalApiException.class)
                .hasMessage("AI 응답을 해석할 수 없습니다.");
    }

    private record DummyResult(boolean ok) {
    }
}
