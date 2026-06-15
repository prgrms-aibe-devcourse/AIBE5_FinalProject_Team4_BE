package com.closetnangam.be.domain.recommendation.support;

import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplementaryRecommendationProductFilterTest {

    @Test
    @DisplayName("안경·옷걸이·가방 등 비의류 상품은 후보에서 제외한다")
    void excludesNonWearableProducts() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("무타슈 우드 옷걸이 20개", "생활/주방", "수납/정리"),
                "무타슈 우드 옷걸이 20개"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("루이비통 안경테", "패션잡화", "안경/선글라스"),
                "루이비통 안경테"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("캔버스 백팩", "패션잡화", "가방"),
                "캔버스 백팩"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("미끄럼방지 옷걸이 맨투맨옷걸이", "패션의류", "남성의류"),
                "미끄럼방지 옷걸이 맨투맨옷걸이"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("3개 남성용 투명 레드 안경, 실내/실외", "패션잡화", "안경/선글라스"),
                "3개 남성용 투명 레드 안경, 실내/실외"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("성인용 EVA 우비 일회용", "생활/주방", "생활용품"),
                "성인용 EVA 우비 일회용"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("남성 레인코트 트렌치", "패션의류", "남성의류"),
                "남성 레인코트 트렌치"
        )).isTrue();
    }

    @Test
    @DisplayName("상의·하의·아우터·신발 카테고리만 허용한다")
    void allowsOnlyCoreCategories() {
        assertThat(ComplementaryRecommendationProductFilter.isAllowedCategory("TOP")).isTrue();
        assertThat(ComplementaryRecommendationProductFilter.isAllowedCategory("BOTTOM")).isTrue();
        assertThat(ComplementaryRecommendationProductFilter.isAllowedCategory("OUTER")).isTrue();
        assertThat(ComplementaryRecommendationProductFilter.isAllowedCategory("SHOES")).isTrue();
        assertThat(ComplementaryRecommendationProductFilter.isAllowedCategory("BAG")).isFalse();
    }

    @Test
    @DisplayName("의류 상품은 후보로 인정한다")
    void acceptsWearableProducts() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("코튼 오버핏 반팔 티셔츠", "패션의류", "남성의류"),
                "코튼 오버핏 반팔 티셔츠"
        )).isTrue();
    }

    @Test
    @DisplayName("네이버 카테고리가 속옷·가방·안경 등이면 상품명과 무관하게 제외한다")
    void excludesByNaverCategory() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("코튼 라운드 티셔츠", "패션의류", "속옷/언더웨어", "팬티"),
                "코튼 라운드 티셔츠"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("데일리 숄더백", "패션잡화", "가방", "숄더백"),
                "데일리 숄더백"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("클래식 안경테", "패션잡화", "안경/선글라스", null),
                "클래식 안경테"
        )).isFalse();
    }

    @Test
    @DisplayName("1+1·1 1 등 묶음 행사 상품은 제외한다")
    void excludesPromoBundleProducts() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("남성 맨투맨 1+1 세트 기모 티셔츠", "패션의류", "남성의류"),
                "남성 맨투맨 1+1 세트 기모 티셔츠"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("여성 와이드 팬츠 1 1 특가", "패션의류", "여성의류"),
                "여성 와이드 팬츠 1 1 특가"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("2+1 맨투맨 3장 구성", "패션의류", "남성의류"),
                "2+1 맨투맨 3장 구성"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("코튼 오버핏 반팔 티셔츠", "패션의류", "남성의류"),
                "코튼 오버핏 반팔 티셔츠"
        )).isTrue();
    }

    @Test
    @DisplayName("니시·나시·이너 런닝 등 속옷류 상품명은 제외한다")
    void excludesUnderwearLikeProducts() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("여성 니시 레이스 속옷 세트", "패션의류", "여성의류"),
                "여성 니시 레이스 속옷 세트"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("남성 런닝 나시 이너웨어", "패션의류", "남성의류"),
                "남성 런닝 나시 이너웨어"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("라네르 남성 베이직 민소매 나시 런닝", "패션의류", "남성의류"),
                "라네르 남성 베이직 민소매 나시 런닝"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("남자반팔티 기능성 쿨 심리스 이너 런닝 티셔츠", "패션의류", "남성의류"),
                "남자반팔티 기능성 쿨 심리스 이너 런닝 티셔츠"
        )).isFalse();
    }

    @Test
    @DisplayName("런닝자켓·런닝 바람막이 등 운동복은 후보로 인정한다")
    void allowsRunningSportswear() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("프로스펙스 남성 바람막이 자켓 점퍼 후드 런닝자켓 윈드브레이커", "패션의류", "남성의류"),
                "프로스펙스 남성 바람막이 자켓 점퍼 후드 런닝자켓 윈드브레이커"
        )).isTrue();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("알티피아 운동 남성 바람막이 런닝 남자 바람막이 아우터 자켓", "패션의류", "남성의류"),
                "알티피아 운동 남성 바람막이 런닝 남자 바람막이 아우터 자켓"
        )).isTrue();
    }

    @Test
    @DisplayName("단추·부자재 등 봉제 부속품은 제외하고 더블버튼 의류는 허용한다")
    void excludesSewingSupplies() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("에이치플러스몰 가디건 금장단추 원형", "패션잡화", "의류부자재"),
                "에이치플러스몰 가디건 금장단추 원형"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("모루인형 단추가디건 니트 상의 장식소품 M-58 단추 1개", "패션의류", "여성의류"),
                "모루인형 단추가디건 니트 상의 장식소품 M-58 단추 1개"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("여자 셔츠 블라우스 단추 연장 소매 목 길이 부속품", "패션의류", "여성의류"),
                "여자 셔츠 블라우스 단추 연장 소매 목 길이 부속품"
        )).isFalse();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("10개 컬러 체크 단추 셔츠 블라우스", "패션의류", "여성의류"),
                "10개 컬러 체크 단추 셔츠 블라우스"
        )).isTrue();

        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("레노마레이디 본사직영 더블단추 하프 트렌치코트", "패션의류", "여성의류"),
                "레노마레이디 본사직영 더블단추 하프 트렌치코트"
        )).isTrue();
    }

    @Test
    @DisplayName("패션잡화 신발 카테고리는 허용한다")
    void allowsShoesUnderFashionMiscCategory() {
        assertThat(ComplementaryRecommendationProductFilter.isWearableCandidate(
                product("화이트 스니커즈", "패션잡화", "남성신발", "스니커즈"),
                "화이트 스니커즈"
        )).isTrue();
    }

    @Test
    @DisplayName("SEO 키워드 나열형·복수 구성 상품은 제외한다")
    void excludesKeywordStuffedListings() {
        assertThat(ComplementaryRecommendationProductFilter.shouldExcludeFromExternalPool(
                "필인 남자 여름 3부 반바지 바람막이쇼츠 남성워크아웃쇼츠 애슬레저하프팬츠",
                "UNKNOWN"
        )).isTrue();

        assertThat(ComplementaryRecommendationProductFilter.shouldExcludeFromExternalPool(
                "벅703 애슬레저룩 기능성 반팔티셔츠 반팔점퍼 팬츠 3종 반팔티 여성바지 남자바지 바람막이",
                "벅703"
        )).isTrue();

        assertThat(ComplementaryRecommendationProductFilter.shouldExcludeFromExternalPool(
                "남성 애슬레저 하프팬츠",
                "나이키"
        )).isFalse();
    }

    @Test
    @DisplayName("바지끈 등 부자재는 제외한다")
    void excludesDrawstringAccessories() {
        assertThat(ComplementaryRecommendationProductFilter.shouldExcludeFromExternalPool(
                "바지 조임끈 세트",
                "바지끈"
        )).isTrue();
    }

    private NaverShoppingProductResponse product(String title, String category2, String category3) {
        return product(title, category2, category3, null);
    }

    private NaverShoppingProductResponse product(String title, String category2, String category3, String category4) {
        return new NaverShoppingProductResponse(
                title,
                "https://example.com",
                "https://example.com/image.jpg",
                10000,
                null,
                "mall",
                "12345",
                "1",
                "brand",
                "maker",
                "패션",
                category2,
                category3,
                category4
        );
    }
}
