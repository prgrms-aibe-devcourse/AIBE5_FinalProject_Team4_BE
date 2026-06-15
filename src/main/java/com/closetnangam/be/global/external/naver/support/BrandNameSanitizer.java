package com.closetnangam.be.global.external.naver.support;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 네이버 brand 필드·과거 title prefix 추론 결과에서 상품 설명성 토큰을 걸러 {@code UNKNOWN}으로 정규화한다.
 */
public final class BrandNameSanitizer {

    private static final String UNKNOWN = "UNKNOWN";

    private static final Pattern GENDER_PREFIX = Pattern.compile("^(남성|여성|남자|여자|남녀|커플|유니섹스)");
    private static final Pattern PRODUCT_DESCRIPTOR = Pattern.compile(
            "슬랙스|팬츠|티셔츠|맨투맨|후드|니트|코트|자켓|재킷|부츠|구두|슈즈|블라우스|가디건|조거|데님|청바지|트렌치|패딩|조끼|베스트|"
                    + "반바지|청반바지|바람막이|면바지|밴딩바지|통바지|정장바지|편한바지|냉장고바지|코끼리바지|주름스커트|"
                    + "스웨트셔츠|스웻셔츠|어셔츠|반팔티|신발|남자신발|여성용|남성용|와이드핏|슬림핏|오버핏|루즈핏|레귤러핏|"
                    + "스트레이트|부츠컷|하이탑|하이넥|하이힐|여름용|핫딜|핫코드|기모안감|여성단화|정석|"
                    + "아웃도어|등산|트레킹|캠핑|트래킹|워킹|하이킹|"
                    + "밀리터리|밀리터리룩|"
                    + "양털|체크|스판|경량|방한|방수|윈드|퍼|플리스|후리스|기능성|보온|린넨|데님|청|"
                    + "윈드브레이커|바람막|경량패딩|발수|방풍|내구|보온성|방한복|등산화|트레킹화|"
                    + "워커|워킹화|로퍼|블로퍼|슬립온|트레이너|캔버스|펌프스|뮬|샌들|플랫|하이탑|골지|카라|"
                    + "트레이닝|트레이닝복|트레이닝웨어|트레이닝바지|트레이닝팬츠|트레이닝자켓|트레이닝점퍼|트레이닝셔츠|트레이닝조거"
    );
    private static final Pattern FIT_DESCRIPTOR_SUFFIX = Pattern.compile(".*(와이드핏|슬림핏|오버핏|루즈핏|레귤러핏|일자핏|드로우핏|레디핏|포레스트핏|테이퍼드핏)$");
    private static final Pattern STYLE_CATEGORY_TOKEN = Pattern.compile(
            "^(?:아웃도어|등산|트레킹|트래킹|캠핑|워킹|하이킹|기능성|방한|방수|경량|정장|데일리|베이직|캐주얼|심플|스포츠|아웃|윈드|"
                    + "방풍|발수|보온|내구|오버|로우|크롭|하이|숏|롱|미니|맥시|하프|풀|세미|슬림|일자|카고|조거|밴딩|핏|"
                    + "체크|스판|양털|플리스|후리스|퍼|린넨|코튼|데님|청|기모|면|울|나일론|가죽|프리미엄|클래식|시즌|"
                    + "신상|인기|추천|정품|공식|매장|세일|할인|특가|무료|배송|국내|해외|원피스|운동화|스니커즈|"
                    + "밀리터리|워커|워킹|블로퍼|슬립온|트레이너|트레이닝|캔버스|펌프스|뮬|샌들|플랫|하이탑|골지|카라|"
                    + "스트릿|빈티지|레트로|모던|포멀|트렌디|블랙|화이트|그레이|베이지|네이비|카키|차콜|아이보리|"
                    + "학생|직장|오피스|공용|세트|기본)(?:한|성|용|복|화|슈즈|룩|스타일|핏|티)?$"
    );
    /** 브랜드명 전체가 상품·카테고리명처럼 끝나는 경우 (예: 양털체크바지, 경량발편한로퍼) */
    private static final Pattern GARMENT_OR_FOOTWEAR_SUFFIX = Pattern.compile(
            ".*(?:바지|팬츠|슬랙스|청바지|반바지|통바지|면바지|밴딩바지|정장바지|치마바지|긴바지|흰바지|기지바지|"
                    + "신발|구두|부츠|로퍼|워커|슈즈|워킹화|등산화|트레킹화|운동화|스니커즈|"
                    + "티셔츠|셔츠|스웨트셔츠|스웻셔츠|어셔츠|반팔티|"
                    + "스커트|원피스|조끼|베스트|점퍼|후드|니트|가디건|민소매|반팔|긴팔|"
                    + "양말|모자|가방|백팩|지갑|벨트|장갑|머플러|스카프|재킷|자켓|코트|패딩|윈드브레이커|바람막이|"
                    + "트레이닝복|트레이닝바지|트레이닝팬츠|트레이닝자켓|트레이닝점퍼|트레이닝셔츠|트레이닝조거)$"
    );

    private static final Set<String> NON_BRAND_TOKENS = Set.of(
            "남자", "여자", "여성", "남성", "남여공용", "남녀공용", "남녀", "커플", "유니섹스",
            "여름", "겨울", "봄", "가을", "시원한", "구김없는", "하이웨스트", "편한", "냉장고",
            "와이드핏", "와이드", "긴팔", "긴팔티", "라운드", "양털", "여성단화", "텐셀",
            "빅사이즈", "기본", "베이직", "심플", "캐주얼", "데일리", "오버핏", "루즈핏",
            "슬림핏", "세미와이드", "신상", "인기", "추천", "정품", "프렌치", "코튼", "린넨",
            "데님", "면", "가죽", "울", "나일롼", "크리스탈", "밴딩", "박스", "반팔", "반바지",
            "맨투맨", "티셔츠", "슬랙스", "팬츠", "바지", "신발", "기모", "바람막이", "클래식",
            "일자", "슬림", "세미", "카고", "크롭", "하프", "로우", "오버", "프리미엄", "시즌",
            "여성용", "남성용", "여름용", "핫딜", "핫코드", "스틸레토", "펌프스", "펌프스힐",
            "통굽", "젤리슈즈", "로퍼", "플랫", "하이탑", "하이넥", "하이힐", "셔츠", "청반바지",
            "남자신발", "남자청반바지", "남자바람막이", "남자냉장고바지", "남성청반바지", "남성워커",
            "남성로퍼", "남성블로퍼", "남성바지", "남성화", "정석와이드핏", "면바지", "밴딩바지",
            "통바지", "정장바지", "편한바지", "냉장고바지", "코끼리바지", "주름스커트", "스웨트셔츠",
            "스웻셔츠", "어셔츠", "반팔티", "기모안감", "착한구두", "발편한", "인조가죽", "소가죽",
            "천연가죽", "블루핏", "레디핏", "드로우핏", "포레스트핏",
            "아웃도어", "OUTDOOR", "양털체크바지", "경량", "정장", "스판", "방한", "방수", "체크", "체크패턴",
            "윈드브레이커", "WINDBREAKER", "청치마", "코튼데이", "심플한", "데일리블루", "타탄체크", "린넨100",
            "린넨셔츠", "세미크롭", "오버사이즈", "밴딩스판", "밴딩와이드치마바지", "엠보싱바지", "긴바지", "흰바지",
            "기지바지", "방수로퍼", "경량발편한로퍼", "여름신상", "봄신상", "시즌세일", "베이직한스타일", "캐주얼하고",
            "심플소잉", "로우게이지", "로우클래식", "퍼안감", "보온성", "내구성", "기능성", "미니", "미니멀", "맥시",
            "테이퍼드핏", "아웃스탠딩", "OUTSTANDING", "퍼펙트", "PERFECT", "청록색",
            "밀리터리", "MILITARY", "워커", "WORKER", "WALKER", "블로퍼", "BLOAFER", "LOAFER",
            "슬립온", "SLIPON", "SLIP-ON", "트레이너", "TRAINER", "골지", "카라", "스트릿", "STREET",
            "빈티지", "VINTAGE", "레트로", "RETRO", "화이트빌딩", "오피스룩", "데일리룩", "기본스타일",
            "기본핏", "기본티", "포멀한", "직장인", "트렌디", "카고브로스", "플랫폼",
            "트레이닝", "TRAINING", "트레이닝복", "트레이닝바지", "트레이닝팬츠", "트레이닝자켓", "트레이닝웨어",
            "바지끈", "바지 끈", "바지조임끈", "조임끈", "허리끈", "신발끈", "바지끈세트"
    );

    /** 상품명·핏 키워드와 겹치지만 실제 브랜드로 유지할 이름 */
    private static final Set<String> PROTECTED_BRANDS = Set.of(
            "더로우", "THEROW", "THE ROW",
            "다이나핏", "DYNAFIT",
            "핏플랍", "FITFLOP",
            "미니멈", "MINIMUM",
            "코오롱스포츠", "KOLONSPORT", "KOLON SPORT",
            "슬로우롤리", "SLOWROLLY",
            "미니멀프로젝트", "MINIMALPROJECT", "MINIMAL PROJECT",
            "베이직바이블", "BASICBIBLE", "BASIC BIBLE",
            "베이직클로즈", "BASICCLOTHES", "BASIC CLOTHES",
            "베이직하우스", "BASICHOUSE", "BASIC HOUSE",
            "풀카운트", "FULLCOUNT", "FULL COUNT",
            "링클프리", "WRINKLEFREE", "WRINKLE FREE",
            "파라점퍼스", "PARAJUMPERS", "PARA JUMPERS",
            "프리즘웍스", "PRISMWORKS", "PRISM WORKS",
            "프리티영띵", "PRETTYNYTHING", "PRETTY NYTHING",
            "프리마클라쎄", "PRIMACLASSE", "PRIMA CLASSE",
            "데일리앤", "DAILYAND", "DAILY AND",
            "오버홀릭", "OVERHOLIC",
            "온나핏", "ONNAFIT",
            "블랙몬스터핏", "BLACKMONSTERFIT", "BLACK MONSTER FIT",
            "하이드로겐", "HYDROGEN",
            "언더스탠딩", "UNDERSTANDING",
            "오프화이트", "OFFWHITE", "OFF-WHITE", "OFF WHITE",
            "블랙야크", "BLACKYAK", "BLACK YAK"
    );

    private BrandNameSanitizer() {
    }

    public static String sanitize(String brandName) {
        if (!StringUtils.hasText(brandName)) {
            return UNKNOWN;
        }

        String trimmed = brandName.trim();
        if (UNKNOWN.equalsIgnoreCase(trimmed)) {
            return UNKNOWN;
        }
        if (isProtectedBrand(trimmed)) {
            return trimmed;
        }
        if (isLikelyProductDescriptor(trimmed)) {
            return UNKNOWN;
        }
        return trimmed;
    }

    public static boolean isLikelyProductDescriptor(String brandName) {
        if (!StringUtils.hasText(brandName)) {
            return false;
        }
        if (isProtectedBrand(brandName)) {
            return false;
        }

        String trimmed = brandName.trim();
        String normalized = normalizeKey(trimmed);

        if (NON_BRAND_TOKENS.contains(trimmed) || NON_BRAND_TOKENS.contains(normalized)) {
            return true;
        }
        if (GENDER_PREFIX.matcher(trimmed).find()) {
            return true;
        }
        if (PRODUCT_DESCRIPTOR.matcher(trimmed).find()) {
            return true;
        }
        if (FIT_DESCRIPTOR_SUFFIX.matcher(trimmed).find()) {
            return true;
        }
        if (STYLE_CATEGORY_TOKEN.matcher(trimmed).find()) {
            return true;
        }
        if (GARMENT_OR_FOOTWEAR_SUFFIX.matcher(trimmed).find()) {
            return true;
        }
        if (trimmed.contains("정석")) {
            return true;
        }
        return trimmed.chars().allMatch(Character::isDigit);
    }

    private static boolean isProtectedBrand(String brandName) {
        String trimmed = brandName.trim();
        if (PROTECTED_BRANDS.contains(trimmed) || PROTECTED_BRANDS.contains(trimmed.toUpperCase(Locale.ROOT))) {
            return true;
        }
        return PROTECTED_BRANDS.contains(normalizeKey(trimmed));
    }

    private static String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s.'\\-&]", "");
    }
}
