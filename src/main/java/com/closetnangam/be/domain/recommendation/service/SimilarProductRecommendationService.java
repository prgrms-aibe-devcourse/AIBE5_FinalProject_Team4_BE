package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.recommendation.dto.response.SimilarProductRecommendationResponse;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.closetnangam.be.global.external.naver.service.NaverApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * 보유 옷 하나를 기준으로 네이버 쇼핑에서 비슷한 상품을 찾는 추천 서비스.
 *
 * 단순히 브랜드명과 상품명을 그대로 검색하면 같은 상품 또는 같은 브랜드 상품만 많이 노출된다.
 * 그래서 이 서비스는 브랜드를 제외하고 성별, 색상, 디자인/핏, 스타일, 아이템 타입 같은 속성 중심의
 * 검색어를 만들어 "동일 상품"보다 "비슷한 분위기의 상품"을 찾는 쪽에 초점을 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimilarProductRecommendationService {

    private static final int RECOMMENDATION_COUNT = 50;
    private static final int SEARCH_DISPLAY_COUNT = 50;
    private static final int SEARCH_PAGE_COUNT = 2;
    private static final List<Integer> SEARCH_START_INDEXES = List.of(1, 51, 101, 151, 201);
    private static final List<String> SEARCH_SORT_OPTIONS = List.of("sim", "date");

    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final NaverApiService naverApiService;

    /**
     * 선택된 옷이 요청한 사용자의 옷인지 확인한 뒤, 속성 기반 검색어로 네이버 쇼핑 상품을 조회한다.
     */
    public SimilarProductRecommendationResponse recommendSimilarProducts(Long userId, Long clothesId) {
        /*
         * 최신 옷장 모델에서는 Clothes가 상품 마스터에 가깝고, 사용자 소유 여부는 WardrobeClothes가 가진다.
         * 따라서 clothesId만 조회하지 않고 userId까지 함께 걸어 "이 사용자의 옷장에 담긴 옷"인지 확인한다.
         */
        WardrobeClothes wardrobeClothes = wardrobeClothesRepository.findByClothesIdAndUserId(clothesId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 옷을 찾을 수 없습니다."));

        Clothes baseClothes = wardrobeClothes.getClothes();
        String query = buildSearchQuery(wardrobeClothes);
        List<NaverShoppingProductResponse> products = searchSimilarProducts(query);

        return new SimilarProductRecommendationResponse(
                ClothesResponse.from(baseClothes, wardrobeClothes),
                query,
                products
        );
    }

    /**
     * 네이버 쇼핑 검색어를 구성한다.
     *
     * 예: ourselves / multi stripe long sleeve / BLACK / MALE / TOP / LONG_SLEEVE
     * -> "남성 블랙 스트라이프 롱슬리브"
     *
     * 브랜드명과 원본 상품명 전체를 넣지 않는 이유:
     * - 브랜드를 넣으면 동일 브랜드 위주로 결과가 좁아진다.
     * - 상품명 전체를 넣으면 동일 상품 재검색에 가까워진다.
     * - 유사 추천에서는 성별, 색상, 디자인, 핏 같은 속성이 더 중요하다.
     */
    private String buildSearchQuery(WardrobeClothes wardrobeClothes) {
        Clothes clothes = wardrobeClothes.getClothes();
        return Stream.of(
                        getGenderLabel(wardrobeClothes),
                        getColorLabel(clothes),
                        extractDesignKeywords(clothes),
                        getPrimaryStyleName(clothes),
                        getItemTypeLabel(clothes)
                )
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + " " + right)
                // 분류 데이터가 모두 비어 있는 비정상 데이터라도 검색 자체는 시도할 수 있게 상품명을 fallback으로 둔다.
                .orElse(clothes.getName());
    }

    /**
     * 새로고침마다 같은 상품만 반복되지 않도록 네이버 검색 페이지와 정렬 기준을 섞어 후보를 모은다.
     *
     * <p>최종 응답도 셔플해 같은 후보군이어도 노출 순서가 고정되지 않게 한다. 네이버 API 기본 검색 개수는
     * 다른 기능에 영향을 줄 수 있으므로 유사상품 추천에서만 50개를 명시적으로 요청한다.</p>
     */
    private List<NaverShoppingProductResponse> searchSimilarProducts(String query) {
        List<Integer> starts = new ArrayList<>(SEARCH_START_INDEXES);
        Collections.shuffle(starts);

        Map<String, NaverShoppingProductResponse> productByKey = new LinkedHashMap<>();
        for (int index = 0; index < Math.min(SEARCH_PAGE_COUNT, starts.size()); index++) {
            String sort = randomSort();
            List<NaverShoppingProductResponse> products = naverApiService.searchShoppingProducts(
                    query,
                    SEARCH_DISPLAY_COUNT,
                    starts.get(index),
                    sort
            );
            for (NaverShoppingProductResponse product : products) {
                productByKey.putIfAbsent(deduplicationKey(product), product);
            }
        }

        List<NaverShoppingProductResponse> shuffledProducts = new ArrayList<>(productByKey.values());
        Collections.shuffle(shuffledProducts);
        if (shuffledProducts.size() <= RECOMMENDATION_COUNT) {
            return shuffledProducts;
        }
        return shuffledProducts.subList(0, RECOMMENDATION_COUNT);
    }

    private String randomSort() {
        return SEARCH_SORT_OPTIONS.get(ThreadLocalRandom.current().nextInt(SEARCH_SORT_OPTIONS.size()));
    }

    private String deduplicationKey(NaverShoppingProductResponse product) {
        if (product.productId() != null && !product.productId().isBlank()) {
            return product.productId().trim();
        }
        if (product.link() != null && !product.link().isBlank()) {
            return product.link().trim();
        }
        return product.title() == null ? "" : product.title().trim();
    }

    /**
     * 성별 키워드는 네이버 쇼핑 결과의 성별 카테고리를 좁히는 데 효과가 크다.
     * OTHER 또는 미입력 상태는 성별을 강제로 제한하지 않는다.
     */
    private String getGenderLabel(WardrobeClothes wardrobeClothes) {
        User.Gender gender = wardrobeClothes.getWardrobe().getUser().getGender();
        if (gender == User.Gender.MALE) {
            return "남성";
        }
        if (gender == User.Gender.FEMALE) {
            return "여성";
        }
        return "";
    }

    /**
     * DB에는 색상 코드(BLACK, WHITE 등)가 저장되므로 쇼핑 검색에 자연스러운 한국어 라벨로 바꾼다.
     * 혹시 enum에 없는 값이 저장되어 있어도 추천 API 전체가 실패하지 않도록 원본 값을 사용한다.
     */
    private String getColorLabel(Clothes clothes) {
        String primaryColorCode = clothes.getSortedColorTags().stream()
                .findFirst()
                .map(ClothingColor::getColorCode)
                .orElse("");

        try {
            return ClothesColor.fromCode(primaryColorCode).getLabel();
        } catch (IllegalArgumentException e) {
            return normalize(primaryColorCode);
        }
    }

    /**
     * 스타일 태그가 여러 개일 수 있지만 검색어가 길어질수록 결과가 과도하게 좁아진다.
     * 현재는 대표 스타일 하나만 사용해 검색 범위를 적당히 유지한다.
     */
    private String getPrimaryStyleName(Clothes clothes) {
        return clothes.getStyleTags().stream()
                .findFirst()
                .map(styleTag -> normalize(styleTag.getStyle().getName()))
                .orElse("");
    }

    /**
     * 상품명에서 브랜드가 아닌 디자인/핏 관련 키워드만 추출한다.
     *
     * 네이버 쇼핑은 한국어 검색어와 영어 상품명이 섞여도 검색되지만, "stripe"보다 "스트라이프"처럼
     * 한국어 패션 키워드를 넣는 편이 국내 쇼핑몰 결과를 넓게 잡는 데 유리하다.
     *
     * 이 로직은 AI 기반 의미 분석이 아니라 MVP용 규칙 기반 추출이다.
     * 키워드를 추가할 때는 너무 특정 브랜드/제품명에 가까운 단어보다 여러 브랜드에 공통으로 쓰이는
     * 패턴, 소재, 핏, 넥라인 중심으로 확장하는 것이 좋다.
     */
    private String extractDesignKeywords(Clothes clothes) {
        String name = normalize(clothes.getName()).toLowerCase();
        return Stream.of(
                        keywordIfContains(name, "multi stripe", "스트라이프"),
                        keywordIfContains(name, "stripe", "스트라이프"),
                        keywordIfContains(name, "striped", "스트라이프"),
                        keywordIfContains(name, "check", "체크"),
                        keywordIfContains(name, "plaid", "체크"),
                        keywordIfContains(name, "denim", "데님"),
                        keywordIfContains(name, "wide", "와이드"),
                        keywordIfContains(name, "slim", "슬림"),
                        keywordIfContains(name, "regular", "레귤러핏"),
                        keywordIfContains(name, "oversize", "오버핏"),
                        keywordIfContains(name, "oversized", "오버핏"),
                        keywordIfContains(name, "crop", "크롭"),
                        keywordIfContains(name, "cropped", "크롭"),
                        keywordIfContains(name, "rib", "골지"),
                        keywordIfContains(name, "ribbed", "골지"),
                        keywordIfContains(name, "henley", "헨리넥"),
                        keywordIfContains(name, "henryneck", "헨리넥"),
                        keywordIfContains(name, "v-neck", "브이넥"),
                        keywordIfContains(name, "v neck", "브이넥"),
                        keywordIfContains(name, "round neck", "라운드넥"),
                        keywordIfContains(name, "crew neck", "라운드넥"),
                        keywordIfContains(name, "turtle", "터틀넥")
                )
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    /**
     * source 안에 특정 영어 키워드가 있으면 네이버 검색용 한국어 표현으로 바꾼다.
     */
    private String keywordIfContains(String source, String keyword, String replacement) {
        if (source.contains(keyword)) {
            return replacement;
        }
        return "";
    }

    /**
     * DB에는 아이템 타입 코드(LONG_SLEEVE 등)가 저장되므로 쇼핑 검색에 적합한 라벨(롱슬리브 등)로 바꾼다.
     */
    private String getItemTypeLabel(Clothes clothes) {
        try {
            return ClothesItemType.fromCode(clothes.getItemType()).getLabel();
        } catch (IllegalArgumentException e) {
            return normalize(clothes.getItemType());
        }
    }

    /**
     * 외부 연동 상품이 아니거나 직접 등록한 옷은 일부 외부 필드가 NONE일 수 있다.
     * 검색어 조합 단계에서는 null/NONE을 빈 값으로 취급해 불필요한 검색어 유입을 막는다.
     */
    private String normalize(String value) {
        if (value == null || value.equalsIgnoreCase("NONE")) {
            return "";
        }
        return value.trim();
    }
}
