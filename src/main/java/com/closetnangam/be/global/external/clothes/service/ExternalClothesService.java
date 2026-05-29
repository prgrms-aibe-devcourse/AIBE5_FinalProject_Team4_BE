package com.closetnangam.be.global.external.clothes.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothesStyleTag;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.enums.SourceType;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.global.external.clothes.dto.NaverItemRequest;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalClothesService {

    private final String DEFAULT_TOP_ITEM_TYPE = "SHORT_SLEEVE";
    private final String DEFAULT_BOTTOM_ITEM_TYPE = "COTTON";
    private final String DEFAULT_CATEGORY = "TOP";
    private final String DEFAULT_OUTER_ITEM_TYPE = "BLAZER";
    private final String DEFAULT_SHOES_ITEM_TYPE = "SNEAKERS";
    private final String DEFAULT_COLOR = "WHITE";
    private final String UNKNOWN = "UNKNOWN";

    private final ClothesRepository clothesRepository;
    private final StyleRepository styleRepository;



    public Long saveNaverToWishlist(Long userId, Wardrobe wardrobe, NaverItemRequest naverRequest,
                                    List<ClothingColorDto> colorDtos, List<ClothesStyleDto> styleDtos) {

        // 1. [기존 가드 로직 유지]
        String productId = defaultIfBlank(naverRequest.getProductId(), UNKNOWN);
        if (!UNKNOWN.equals(productId) && clothesRepository.existsByExternalProductId(productId)) {
            throw new IllegalStateException("이미 존재하는 상품입니다: " + productId);
        }

        // 2. [Clothes 엔티티 생성]
        Clothes clothes = Clothes.builder()
                .name(naverRequest.getCleanTitle())
                .brandName(hasText(naverRequest.getBrand()) ? naverRequest.getBrand().trim() : UNKNOWN)
                .productCode("NAVER_" + productId)
                .imageUrl(naverRequest.getImage())
                .category(refineCategory(naverRequest.getCategory3()))
                .itemType(refineItemType(refineCategory(naverRequest.getCategory3()), naverRequest.getCategory3(), naverRequest.getCleanTitle()))
                .sourceType(SourceType.WISHLIST)
                .externalSource("NAVER")
                .externalProductId(productId)
                .externalProductUrl(naverRequest.getLink())
                .isVerified(false)
                .build();

        // 3. [색상 태그 저장]
        for (ClothingColorDto dto : colorDtos) {
            ClothingColor colorTag = ClothingColor.create(
                    clothes,
                    dto.colorCode(),    // 괄호 이름 그대로!
                    dto.colorRole(),
                    dto.sortOrder()
            );
            clothes.addColorTag(colorTag);
        }

        // 4. [스타일 태그 저장]
        for (ClothesStyleDto dto : styleDtos) {
            Style style = styleRepository.findById(dto.styleId()) // 여기서도 dto.styleId()
                    .orElseThrow(() -> new IllegalArgumentException("스타일 없음"));

            ClothesStyleTag styleTag = ClothesStyleTag.create(
                    clothes,
                    style,
                    dto.styleRole(),
                    dto.sortOrder()
            );
            clothes.addStyleTag(styleTag);
        }

        // 5. [Cascade 저장]
        // Clothes 엔티티에 @OneToMany(cascade = CascadeType.ALL)이 있으므로,
        // clothes만 저장해도 색상/스타일 태그가 함께 DB에 Insert됨!
        return clothesRepository.save(clothes).getId();
    }
    private String refineCategory(String naverCategory3) {
        String categoryText = normalizeText(naverCategory3);
        if (categoryText.isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        if (containsAny(categoryText, "신발", "구두", "슈즈", "스니커즈", "운동화", "로퍼", "더비", "부츠", "샌들", "슬리퍼", "힐", "플랫")) {
            return "SHOES";
        }

        if (containsAny(categoryText, "부츠컷")) {
            return "BOTTOM";
        }

        if (containsAny(categoryText, "바지", "팬츠", "슬랙스", "데님", "청바지", "스커트", "치마", "쇼츠", "반바지", "카고", "조거")) {
            return "BOTTOM";
        }

        if (containsAny(categoryText, "패딩", "코트", "자켓", "재킷", "점퍼", "바람막이", "집업", "블루종", "블레이저", "무스탕", "베스트", "조끼", "야상", "아우터")) {
            return "OUTER";
        }

        if (containsAny(categoryText, "셔츠", "티셔츠", "맨투맨", "후드티", "니트", "스웨터", "가디건", "블라우스", "민소매", "나시", "카라", "폴로", "탑")) {
            return "TOP";
        }

        return DEFAULT_CATEGORY;
    }

    private String refineItemType(String category, String naverCategory3, String title) {
        String lookupText = normalizeText(naverCategory3) + normalizeText(title);

        if ("TOP".equals(category)) {
            return refineTopItemType(lookupText);
        }
        if ("BOTTOM".equals(category)) {
            return refineBottomItemType(lookupText);
        }
        if ("OUTER".equals(category)) {
            return refineOuterItemType(lookupText);
        }
        if ("SHOES".equals(category)) {
            return refineShoesItemType(lookupText);
        }

        return DEFAULT_TOP_ITEM_TYPE;
    }

    private String refineColor(String title) {
        String colorText = normalizeText(title);
        if (colorText.isEmpty()) {
            return DEFAULT_COLOR;
        }

        if (containsAny(colorText, "black", "blk", "블랙", "검정", "검은", "흑", "까만", "먹색", "올블랙", "제트블랙", "noir", "흑청")) {
            return "BLACK";
        }
        if (containsAny(colorText, "gray", "grey", "그레이", "회색", "차콜", "멜란지", "메란지", "애쉬", "ash", "그레이지", "실버", "스모크")) {
            return "GRAY";
        }
        if (containsAny(colorText, "navy", "네이비", "남색", "곤색", "인디고", "딥블루", "다크블루", "미드나잇", "midnight", "진청", "딥네이비")) {
            return "NAVY";
        }
        if (containsAny(colorText, "lightblue", "라이트블루", "연청", "중청", "연파랑", "하늘", "스카이", "스카이블루", "소라", "아이스블루", "파스텔블루")) {
            return "LIGHT_BLUE";
        }
        if (containsAny(colorText, "purple", "퍼플", "보라", "보랏빛", "바이올렛", "violet", "라벤더", "라일락", "자주", "플럼")) {
            return "PURPLE";
        }
        if (containsAny(colorText, "red", "레드", "빨강", "빨간", "붉은", "와인", "wine", "버건디", "burgundy", "체리", "마룬", "maroon")) {
            return "RED";
        }
        if (containsAny(colorText, "orange", "오렌지", "주황", "코랄", "coral", "살구", "테라코타", "terracotta")) {
            return "ORANGE";
        }
        if (containsAny(colorText, "yellow", "옐로", "옐로우", "노랑", "노란", "머스타드", "머스터드", "gold", "골드", "레몬")) {
            return "YELLOW";
        }
        if (containsAny(colorText, "green", "그린", "초록", "민트", "mint", "카키", "khaki", "올리브", "olive", "리프")) {
            return "GREEN";
        }
        if (containsAny(colorText, "brown", "브라운", "갈색", "밤색", "카멜", "camel", "모카", "mocha", "초코", "커피", "tan", "탠")) {
            return "BROWN";
        }
        if (containsAny(colorText, "beige", "베이지", "샌드", "sand", "누드베이지", "연베이지", "내추럴", "natural", "스킨", "skin", "에크루", "ecru", "라떼")) {
            return "BEIGE";
        }
        if (containsAny(colorText, "pink", "핑크", "분홍", "연핑크", "로즈", "rose", "피치", "peach", "살몬", "salmon")) {
            return "PINK";
        }
        if (containsAny(colorText, "white", "화이트", "흰색", "흰", "백색", "오프화이트", "아이보리", "ivory", "크림", "cream", "에크루", "ecru")) {
            return "WHITE";
        }

        return DEFAULT_COLOR;
    }

    private String refineTopItemType(String lookupText) {
        if (containsAny(lookupText, "후드티", "후디", "hoodie", "hooded", "후드맨투맨", "후드")) {
            return "HOODIE";
        }
        if (containsAny(lookupText, "맨투맨", "스웨트", "스웻", "sweatshirt", "쭈리", "기모맨투맨")) {
            return "SWEAT";
        }
        if (containsAny(lookupText, "카라티", "카라티셔츠", "피케", "pk티", "폴로티", "폴로", "polo", "piquet")) {
            return "COLLAR_TEE";
        }
        if (containsAny(lookupText, "민소매", "나시", "슬리브리스", "sleeveless", "탱크탑", "탱크", "뷔스티에", "튜브탑")) {
            return "SLEEVELESS";
        }
        if (containsAny(lookupText, "니트", "스웨터", "가디건", "knit", "sweater", "울", "캐시미어", "꽈배기", "터틀넥니트")) {
            return "KNIT";
        }
        if (containsAny(lookupText, "블라우스", "남방", "와이셔츠", "오버셔츠", "린넨셔츠", "데님셔츠", "체크셔츠", "옥스퍼드셔츠", "플란넬셔츠")
                || (lookupText.contains("셔츠") && !lookupText.contains("티셔츠"))) {
            return "SHIRT";
        }
        if (containsAny(lookupText, "긴팔", "롱슬리브", "longsleeve", "긴소매", "긴팔티", "목폴라", "터틀넥", "폴라티", "하프넥", "베이스레이어", "이너티")) {
            return "LONG_SLEEVE";
        }
        if (containsAny(lookupText, "반팔", "반소매", "숏슬리브", "shortsleeve", "반팔티", "반팔티셔츠", "크롭티", "티셔츠", "tee")) {
            return "SHORT_SLEEVE";
        }

        return DEFAULT_TOP_ITEM_TYPE;
    }

    private String refineBottomItemType(String lookupText) {
        if (containsAny(lookupText, "스커트", "치마", "미니스커트", "롱스커트", "플리츠스커트", "랩스커트", "주름치마")) {
            return "SKIRT";
        }
        if (containsAny(lookupText, "카고", "cargo", "멀티포켓", "포켓팬츠")) {
            return "CARGO";
        }
        if (containsAny(lookupText, "슬랙스", "슬렉스", "정장바지", "드레스팬츠", "수트팬츠", "수트바지", "테이퍼드", "핀턱")) {
            return "SLACKS";
        }
        if (containsAny(lookupText, "트레이닝", "츄리닝", "조거", "트레이닝팬츠", "트랙팬츠", "저지팬츠", "스웻팬츠", "스포츠팬츠")) {
            return "TRAINING";
        }
        if (containsAny(lookupText, "반바지", "쇼츠", "숏팬츠", "숏츠", "하프팬츠", "버뮤다", "핫팬츠", "데님쇼츠", "카고쇼츠", "치노쇼츠")) {
            return "SHORTS";
        }
        if (containsAny(lookupText, "데님", "청바지", "jeans", "jean", "생지", "워싱", "연청", "중청", "진청", "흑청", "데님팬츠")) {
            return "DENIM";
        }
        if (containsAny(lookupText, "면바지", "코튼", "코튼팬츠", "면팬츠", "치노", "치노팬츠", "면슬랙스")) {
            return "COTTON";
        }

        return DEFAULT_BOTTOM_ITEM_TYPE;
    }

    private String refineOuterItemType(String lookupText) {
        if (containsAny(lookupText, "후드집업", "집업후드", "후드지퍼", "zipuphoodie", "hoodzip", "hoodzipup")) {
            return "HOOD_ZIPUP";
        }
        if (containsAny(lookupText, "경량패딩", "초경량패딩", "라이트패딩", "경량다운", "초경량다운", "얇은패딩", "얇은다운", "경량구스", "경량오리털")) {
            return "LIGHT_PADDING";
        }
        if (containsAny(lookupText, "패딩", "다운자켓", "다운점퍼", "롱패딩", "숏패딩", "구스다운", "오리털", "퀼팅패딩", "솜패딩", "패딩점퍼")) {
            return "PADDING";
        }
        if (containsAny(lookupText, "무스탕", "시어링", "shearling", "양털자켓", "양털코트", "램스킨", "무톤", "퍼무스탕")) {
            return "SHEARLING";
        }
        if (containsAny(lookupText, "플리스", "후리스", "fleece", "뽀글이", "보아", "boa", "쉐르파", "셰르파", "양털후리스")) {
            return "FLEECE_JACKET";
        }
        if (containsAny(lookupText, "가죽자켓", "가죽점퍼", "레더자켓", "레더점퍼", "레더", "leather", "라이더", "라이더스", "바이커")) {
            return "LEATHER_JACKET";
        }
        if (containsAny(lookupText, "데님자켓", "데님재킷", "청자켓", "청재킷", "트러커", "denimjacket")) {
            return "DENIM_JACKET";
        }
        if (containsAny(lookupText, "바시티", "스타디움", "레터맨", "야구점퍼", "야구자켓", "스쿨자켓", "baseball")) {
            return "VARSITY_JACKET";
        }
        if (containsAny(lookupText, "ma1", "ma-1", "ma 1", "항공점퍼", "항공", "플라이트", "파일럿", "flight")) {
            return "MA1";
        }
        if (containsAny(lookupText, "블루종", "블루존", "blouson", "봄버", "bomber", "바머")) {
            return "BLOUSON";
        }
        if (containsAny(lookupText, "트레이닝자켓", "트레이닝점퍼", "트랙자켓", "트랙점퍼", "저지자켓", "져지자켓", "트레이닝집업", "트랙탑")) {
            return "TRAINING_JACKET";
        }
        if (containsAny(lookupText, "바람막이", "윈드브레이커", "windbreaker", "아노락", "우븐", "러닝자켓")) {
            return "WINDBREAKER";
        }
        if (containsAny(lookupText, "코치자켓", "코치재킷", "코치", "스냅자켓", "스냅버튼", "나일론코치", "coach")) {
            return "COACH_JACKET";
        }
        if (containsAny(lookupText, "워크자켓", "필드자켓", "야상", "사파리", "헌팅", "밀리터리", "m65", "m-65")) {
            return "WORK_JACKET";
        }
        if (containsAny(lookupText, "조끼", "베스트", "vest", "패딩조끼", "경량조끼", "니트베스트", "퀼팅베스트")) {
            return "VEST";
        }
        if (containsAny(lookupText, "떡볶이코트", "더플코트", "더플", "duffle", "토글", "토글코트")) {
            return "TTEOKBOKKI_COAT";
        }
        if (containsAny(lookupText, "더블코트", "더블버튼", "더블")) {
            return "DOUBLE_COAT";
        }
        if (containsAny(lookupText, "발마칸", "발마칸코트", "balmacaan", "맥코트")) {
            return "BALMACAAN_COAT";
        }
        if (containsAny(lookupText, "싱글코트", "싱글버튼", "singlecoat", "싱글")) {
            return "SINGLE_COAT";
        }
        if (containsAny(lookupText, "블레이저", "테일러드", "수트자켓", "정장자켓", "셋업자켓", "수트재킷", "재킷", "자켓")) {
            return "BLAZER";
        }
        if (containsAny(lookupText, "코트")) {
            return "SINGLE_COAT";
        }

        return DEFAULT_OUTER_ITEM_TYPE;
    }

    private String refineShoesItemType(String lookupText) {
        if (containsAny(lookupText, "힐", "하이힐", "펌프스", "스틸레토", "슬링백힐", "스트랩힐", "미들힐", "웨지힐", "플랫폼힐")) {
            return "HEEL";
        }
        if (containsAny(lookupText, "플랫", "플랫슈즈", "발레리나", "단화", "메리제인")) {
            return "FLAT";
        }
        if (containsAny(lookupText, "샌들", "슬리퍼", "뮬", "블로퍼", "쪼리", "플립플랍", "오픈토", "슬링백")) {
            return "SANDALS_SLIPPERS";
        }
        if (containsAny(lookupText, "부츠", "첼시", "워커", "앵클", "롱부츠", "미들부츠", "레인부츠", "마틴", "웨스턴", "하이커")) {
            return "BOOTS";
        }
        if (containsAny(lookupText, "더비슈즈", "더비", "옥스퍼드", "옥스포드", "레이스업구두", "레이스업")) {
            return "DERBY";
        }
        if (containsAny(lookupText, "로퍼", "태슬로퍼", "페니로퍼", "드라이빙로퍼", "로퍼슈즈")) {
            return "LOAFER";
        }
        if (containsAny(lookupText, "러닝화", "런닝화", "트레이닝화", "스포츠슈즈", "조깅화", "워킹화", "축구화", "풋살화", "테니스화", "배드민턴화", "농구화", "골프화", "트레일화", "등산화")) {
            return "SPORTS_SHOES";
        }
        if (containsAny(lookupText, "스니커즈", "운동화", "캔버스", "슬립온", "컨버스", "하이탑", "로우탑", "코트화", "데일리슈즈")) {
            return "SNEAKERS";
        }

        return DEFAULT_SHOES_ITEM_TYPE;
    }

    private String normalizeText(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
