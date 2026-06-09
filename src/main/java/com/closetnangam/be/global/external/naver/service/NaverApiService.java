package com.closetnangam.be.global.external.naver.service;

import com.closetnangam.be.global.common.exception.ExternalApiException;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingSearchResponse;
import com.closetnangam.be.global.external.naver.support.NaverShoppingTitleSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 네이버 Open API 연동 서비스.
 *
 * 기존 테스트용 문자열 응답(searchShop)은 유지하고, 추천 기능에서는 타입이 있는 응답
 * searchShoppingProducts를 사용한다. 외부 API 원본 구조를 컨트롤러까지 그대로 노출하지 않기 위해
 * 이 서비스에서 네이버 응답을 애플리케이션 응답 DTO로 정리한다.
 */
@Service
@RequiredArgsConstructor
public class NaverApiService {

    private static final int DEFAULT_DISPLAY_COUNT = 10;
    private static final int DEFAULT_START_INDEX = 1;
    private static final String DEFAULT_SORT = "sim";
    private static final String NAVER_SHOPPING_PATH = "/v1/search/shop.json";

    private final RestTemplate restTemplate;

    // ⚠️ 중요: yml 경로(spring.security.oauth2.client.registration.naver...)와 완벽하게 싱크를 맞춥니다!
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    /**
     * 기존 /api/naver/search 테스트 엔드포인트에서 사용하는 원본 JSON 반환 메서드.
     * 새 추천 API는 아래의 searchShoppingProducts를 사용한다.
     */
    public String searchShop(String keyword) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    buildShoppingSearchUri(keyword, DEFAULT_DISPLAY_COUNT),
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            throw new ExternalApiException("네이버 쇼핑 API 호출 실패: " + response.getStatusCode());
        } catch (RestClientException e) {
            throw new ExternalApiException("네이버 쇼핑 API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 추천 기능에서 사용하는 네이버 쇼핑 검색 메서드.
     * 네이버 원본 응답을 바로 반환하지 않고, 프론트 추천 카드에 필요한 필드만 정제해서 반환한다.
     */
    public List<NaverShoppingProductResponse> searchShoppingProducts(String keyword) {
        return searchShoppingProducts(keyword, DEFAULT_DISPLAY_COUNT, DEFAULT_START_INDEX, DEFAULT_SORT);
    }

    /**
     * RECO-004 시드 등에서 키워드·페이지·정렬을 바꿔 다양한 후보를 수집할 때 사용한다.
     * start는 1-based이며 display와 함께 페이지를 결정한다. (예: display=10, start=11 → 2페이지)
     */
    public List<NaverShoppingProductResponse> searchShoppingProducts(
            String keyword,
            int display,
            int start,
            String sort
    ) {
        return searchShoppingProducts(keyword, display, start, sort, null);
    }

    /**
     * {@code exclude} 예: {@code used:rental:cbshop} (중고·렌탈·해외직구 제외).
     */
    public List<NaverShoppingProductResponse> searchShoppingProducts(
            String keyword,
            int display,
            int start,
            String sort,
            String exclude
    ) {
        try {
            ResponseEntity<NaverShoppingSearchResponse> response = restTemplate.exchange(
                    buildShoppingSearchUri(keyword, display, start, sort, exclude),
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    NaverShoppingSearchResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ExternalApiException("네이버 쇼핑 API 호출 실패: " + response.getStatusCode());
            }

            // 네이버가 정상 응답을 주더라도 items가 비어 있을 수 있으므로 빈 리스트로 정규화한다.
            List<NaverShoppingSearchResponse.NaverShoppingItem> items = response.getBody().items();
            if (items == null) {
                return List.of();
            }

            return items.stream()
                    .map(this::toProductResponse)
                    .toList();
        } catch (RestClientException e) {
            throw new ExternalApiException("네이버 쇼핑 API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검색어와 노출 개수를 네이버 쇼핑 검색 API URI로 변환한다.
     * UriComponentsBuilder의 encode를 사용해 한글 검색어를 안전하게 인코딩한다.
     */
    private URI buildShoppingSearchUri(String keyword, int displayCount) {
        return buildShoppingSearchUri(keyword, displayCount, DEFAULT_START_INDEX, DEFAULT_SORT, null);
    }

    private URI buildShoppingSearchUri(String keyword, int displayCount, int start, String sort) {
        return buildShoppingSearchUri(keyword, displayCount, start, sort, null);
    }

    private URI buildShoppingSearchUri(String keyword, int displayCount, int start, String sort, String exclude) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path(NAVER_SHOPPING_PATH)
                .queryParam("query", keyword)
                .queryParam("display", displayCount)
                .queryParam("start", start)
                .queryParam("sort", sort);
        if (exclude != null && !exclude.isBlank()) {
            builder.queryParam("exclude", exclude);
        }
        return builder
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    /**
     * 네이버 쇼핑 Open API 인증 헤더.
     * 현재 프로젝트의 네이버 OAuth 설정값과 같은 환경변수를 사용한다.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 네이버 원본 item을 서비스 응답 DTO로 변환한다.
     *
     * 여기서 title HTML 태그 제거와 가격 숫자 변환을 처리해 컨트롤러/프론트가 네이버 원본 포맷을
     * 직접 알 필요가 없도록 한다.
     */
    private NaverShoppingProductResponse toProductResponse(NaverShoppingSearchResponse.NaverShoppingItem item) {
        NaverShoppingTitleSanitizer.Result sanitized = NaverShoppingTitleSanitizer.sanitize(
                item.title(),
                item.productId()
        );
        return new NaverShoppingProductResponse(
                sanitized.displayName(),
                item.link(),
                item.image(),
                parsePrice(item.lprice()),
                parsePrice(item.hprice()),
                item.mallName(),
                item.productId(),
                item.productType(),
                item.brand(),
                item.maker(),
                item.category1(),
                item.category2(),
                item.category3(),
                item.category4()
        );
    }

    /**
     * 네이버 쇼핑 검색 결과 title에는 검색어 강조용 <b> 태그가 포함될 수 있다.
     * 화면에는 순수 상품명만 보여주기 위해 간단히 HTML 태그를 제거한다.
     */
    private String removeHtmlTags(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "");
    }

    /**
     * 네이버 가격 필드는 숫자처럼 보이지만 문자열로 내려온다.
     * 값이 비어 있거나 예상치 못한 포맷이면 추천 응답 전체를 실패시키지 않고 null로 둔다.
     */
    private Integer parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
