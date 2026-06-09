package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.domain.catalog.enums.ClothesCategory;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * RECO-004 외부 후보용 비의류·액세서리 필터.
 * 네이버 카테고리(category1~4)와 상품명 키워드로 걸러 상의·하의·아우터·신발 후보만 적재한다.
 */
public final class ComplementaryRecommendationProductFilter {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            ClothesCategory.TOP.name(),
            ClothesCategory.BOTTOM.name(),
            ClothesCategory.OUTER.name(),
            ClothesCategory.SHOES.name()
    );

    /**
     * 네이버 쇼핑 category1~4에 포함되면 제외할 분류 키워드.
     * 패션잡화 전체는 신발 카테고리가 있어 제외하지 않고, 세분류 단위로 막는다.
     */
    private static final String[] EXCLUDED_NAVER_CATEGORY_KEYWORDS = {
            "속옷", "언더웨어", "이너웨어", "브래지어", "브라", "팬티", "니시", "니삭스", "내복", "런닝",
            "드로즈", "브리프", "캐미솔", "슬립", "란제리", "이너팬츠", "속바지", "보정속옷", "거들", "코르셋",
            "잠옷/홈웨어", "잠옷",
            "가방", "백팩", "클러치", "파우치", "에코백", "숄더백", "크로스백", "토트백", "쇼퍼백",
            "안경", "선글라스",
            "모자", "비니", "버킷햇",
            "양말", "스타킹", "레깅스",
            "시계", "워치",
            "주얼리", "악세서리", "액세서리",
            "벨트",
            "넥타이",
            "장갑",
            "스카프", "머플러",
            "지갑",
            "우산",
            "가발",
            "수납/정리", "생활/주방", "생활용품",
            "화장품", "뷰티", "향수",
            "침구", "커튼", "이불", "베개",
            "세탁", "빨래",
            "부자재", "봉제", "소잉", "리폼부자재"
    };

    private static final String[] EXCLUDED_TITLE_KEYWORDS = {
            "안경", "선글라스", "glasses", "eyewear", "sunglass", "goggle",
            "옷걸이", "옷걸이링", "맨투맨옷걸이", "니트옷걸이", "논슬립옷걸이", "미끄럼방지옷걸이",
            "행거", "hanger", "코트행거", "바지걸이", "사이즈고리",
            "가방", "백팩", "backpack", "숄더백", "크로스백", "토트백", "클러치", "파우치", "에코백", "쇼퍼백",
            "모자", "캡", "비니", "버킷햇", "hat", "cap", "beanie",
            "양말", "스타킹", "socks", "stocking",
            "벨트", "belt",
            "시계", "watch", "워치",
            "목걸이", "귀걸이", "팔찌", "반지", "주얼리", "jewelry", "necklace", "bracelet", "earring",
            "스카프", "머플러", "scarf", "muffler",
            "장갑", "glove", "mitten",
            "지갑", "wallet", "카드지갑",
            "넥타이", "tie", "bowtie",
            "우산", "umbrella",
            "휴대폰", "폰케이스", "phone case",
            "악세서리", "액세서리", "accessory", "accessories",
            "속옷", "언더웨어", "underwear", "브라", "팬티", "lingerie",
            "니시", "니삭스", "내복", "드로즈", "브리프", "캐미솔", "슬립", "란제리",
            "이너팬츠", "속바지", "보정속옷", "거들", "코르셋", "브라렛", "브라팬티", "트렁크",
            "삼각팬티", "사각팬티", "여성속옷", "남성속옷", "이너웨어세트",
            "수건", "타월", "이불", "침구", "베개",
            "세탁", "빨래", "건조대", "세제",
            "스티커", "패치", "와펜",
            "가발", "wig",
            "마스크팩", "화장품", "cosmetic", "perfume", "향수",
            "1+1", "2+1", "3+1", "1+2", "4+1", "1+1세트", "2+1세트", "3+1세트",
            "원플러스원", "1플러스1", "2플러스1", "3플러스1", "1plus1", "2plus1",
            "제작건", "단체티", "단체복", "인쇄비포함", "기본인쇄", "주문제작", "프린트스타", "맞춤인쇄", "소량프린팅",
            "부속품", "부자재", "장식소품", "금장단추", "코팅단추", "단추세트", "봉제단추", "스냅단추", "누름단추",
            "프레스단추", "단추부자재", "지퍼부자재", "의류부자재"
    };

    /**
     * 공백으로 표기되는 1+1 행사. (정규화 시 "11"과 구분하기 위해 별도 정규식 사용)
     */
    private static final Pattern PROMO_BUNDLE_PATTERN = Pattern.compile(
            "(?i)(?:\\d\\s*\\+\\s*\\d|1\\s+1(?:\\s*(?:입|개|세트|팩|장|구성|특가|행사|박스|봉))?)"
    );

    /** 런닝자켓·런닝복 등 운동복은 허용한다. */
    private static final Pattern RUNNING_SPORT_COMPOUND_PATTERN = Pattern.compile(
            "런닝(?:자켓|점퍼|복|화|웨어|조끼|브레이커|윈드|트레이닝|숏츠|쇼츠|반바지|바지|팬츠)"
    );

    /** 속옷 맥락의 나시·런닝만 제외한다. */
    private static final Pattern UNDERWEAR_TANK_OR_RUNNING_PATTERN = Pattern.compile(
            "나시|(?:이너|심리스).{0,15}런닝|런닝.{0,15}(?:이너|나시)|면런닝|순면런닝"
    );

    /** 단추 N개 등 부자재 판매. 뒤에 셔츠·코트 등 의류명이 있으면 허용한다. */
    private static final Pattern BUTTON_SUPPLY_COUNT_PATTERN = Pattern.compile(
            "단추\\s*\\d+\\s*개|\\d+\\s*개\\s*(?:컬러\\s*)?(?:체크\\s*)?단추"
    );

    private static final Pattern CLOTHING_GARMENT_PATTERN = Pattern.compile(
            "셔츠|블라우스|코트|자켓|가디건|팬츠|바지|니트|맨투맨|티셔츠|후드|원피스|치마|스커트|신발|스니커|부츠|조끼|베스트|트렌치|점퍼|슬랙스|데님|청바지|반팔|긴팔|와이셔츠"
    );

    private ComplementaryRecommendationProductFilter() {
    }

    public static boolean isAllowedCategory(String category) {
        return StringUtils.hasText(category) && ALLOWED_CATEGORIES.contains(category.trim());
    }

    public static boolean isWearableCandidate(NaverShoppingProductResponse product, String cleanTitle) {
        if (product == null) {
            return false;
        }
        if (hasExcludedNaverCategory(product)) {
            return false;
        }
        if (hasPromoBundleNotation(cleanTitle, product.title())) {
            return false;
        }
        if (hasUnderwearTankOrRunning(cleanTitle, product.title())) {
            return false;
        }
        if (isSewingSupply(cleanTitle, product.title())) {
            return false;
        }
        return !containsExcludedKeyword(buildTitleSearchableText(product, cleanTitle), EXCLUDED_TITLE_KEYWORDS);
    }

    private static boolean isSewingSupply(String... texts) {
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            String stripped = text.replaceAll("<[^>]*>", "");
            if (!BUTTON_SUPPLY_COUNT_PATTERN.matcher(stripped).find()) {
                continue;
            }
            if (!CLOTHING_GARMENT_PATTERN.matcher(normalize(stripped)).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnderwearTankOrRunning(String... texts) {
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            String normalized = normalize(text);
            if (UNDERWEAR_TANK_OR_RUNNING_PATTERN.matcher(normalized).find()) {
                return true;
            }
            if (!normalized.contains("런닝")) {
                continue;
            }
            if (RUNNING_SPORT_COMPOUND_PATTERN.matcher(normalized).find()) {
                continue;
            }
            if (isRunningOuterContext(normalized)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isRunningOuterContext(String normalized) {
        return normalized.contains("바람막이")
                || normalized.contains("윈드브레이커")
                || normalized.contains("윈드")
                || normalized.contains("아우터");
    }

    private static boolean hasPromoBundleNotation(String... texts) {
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            String stripped = text.replaceAll("<[^>]*>", "");
            if (PROMO_BUNDLE_PATTERN.matcher(stripped).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExcludedNaverCategory(NaverShoppingProductResponse product) {
        String categoryText = buildNaverCategoryText(product);
        return containsExcludedKeyword(categoryText, EXCLUDED_NAVER_CATEGORY_KEYWORDS);
    }

    private static String buildNaverCategoryText(NaverShoppingProductResponse product) {
        StringBuilder builder = new StringBuilder();
        appendNormalized(builder, product.category1());
        appendNormalized(builder, product.category2());
        appendNormalized(builder, product.category3());
        appendNormalized(builder, product.category4());
        return builder.toString();
    }

    private static String buildTitleSearchableText(NaverShoppingProductResponse product, String cleanTitle) {
        StringBuilder builder = new StringBuilder();
        appendNormalized(builder, cleanTitle);
        appendNormalized(builder, product.title());
        appendNormalized(builder, product.brand());
        return builder.toString();
    }

    private static void appendNormalized(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(normalize(value));
    }

    private static boolean containsExcludedKeyword(String searchableText, String[] keywords) {
        if (!StringUtils.hasText(searchableText)) {
            return false;
        }
        for (String keyword : keywords) {
            if (searchableText.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value
                .replaceAll("<[^>]*>", "")
                .toLowerCase()
                .replaceAll("\\s+", "");
    }
}

