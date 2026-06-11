package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.ClothesColor;
import com.closetnangam.be.domain.catalog.enums.ClothesItemType;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.Clothes;
import com.closetnangam.be.domain.clothes.entity.ClothingColor;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.ColorRole;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import com.closetnangam.be.domain.clothes.enums.StyleRole;
import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.clothes.repository.WardrobeClothesRepository;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitItemRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.recommendation.dto.request.AiMdOutfitSaveRequest;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdGeminiOutfitResult;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdGeminiProductResult;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdOutfitRecommendationResponse;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdOutfitRecommendationResponse.OutfitRecommendation;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdOutfitRecommendationResponse.SavedOutfitRecommendation;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdPersonaResponse;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdProductRecommendationResponse;
import com.closetnangam.be.domain.recommendation.dto.response.AiMdProductRecommendationResponse.ProductRecommendation;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.global.external.clothes.dto.record.NaverProductCreateRequest;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import com.closetnangam.be.global.external.gemini.GeminiService;
import com.closetnangam.be.global.external.naver.dto.NaverShoppingProductResponse;
import com.closetnangam.be.global.external.naver.service.NaverApiService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiMdRecommendationService {

    private static final int OUTFIT_COUNT = 4;
    private static final int PRODUCT_RECOMMENDATION_COUNT = 10;
    private static final int MAX_WARDROBE_ITEMS_FOR_PROMPT = 24;
    private static final int OUTFIT_PRODUCTS_PER_CATEGORY = 10;
    private static final Set<String> REQUIRED_OUTFIT_CATEGORIES = Set.of("TOP", "BOTTOM", "SHOES");
    private static final Map<String, String> OUTFIT_CATEGORY_SEARCH_KEYWORDS = Map.of(
            "TOP", "티셔츠",
            "BOTTOM", "팬츠",
            "OUTER", "자켓",
            "SHOES", "스니커즈"
    );

    private final UserRepository userRepository;
    private final WardrobeClothesRepository wardrobeClothesRepository;
    private final OutfitBookRepository outfitBookRepository;
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final ClothesRepository clothesRepository;
    private final StyleRepository styleRepository;
    private final NaverApiService naverApiService;
    private final ExternalClothesService externalClothesService;
    private final GeminiService geminiService;

    public List<AiMdPersonaResponse> getPersonas(Long userId) {
        User user = findUser(userId);
        return AiMdPersona.responsesFor(user.getGender());
    }

    @Transactional
    public AiMdOutfitRecommendationResponse recommendOutfits(Long userId, String mdId) {
        User user = findUser(userId);
        AiMdPersona persona = resolvePersonaForUser(user, mdId);
        List<WardrobeClothes> wardrobeItems = findOwnedWardrobeItems(userId);
        List<NaverShoppingProductResponse> externalProducts = searchOutfitProductsByCategory(persona);

        AiMdGeminiOutfitResult aiResult = geminiService.generateJsonFromText(
                buildOutfitPrompt(persona, wardrobeItems, externalProducts),
                AiMdGeminiOutfitResult.class
        );

        Map<Long, WardrobeClothes> wardrobeById = wardrobeItems.stream()
                .collect(Collectors.toMap(WardrobeClothes::getId, Function.identity(), (left, right) -> left));
        Map<String, NaverShoppingProductResponse> productById = externalProducts.stream()
                .filter(product -> StringUtils.hasText(product.productId()))
                .collect(Collectors.toMap(NaverShoppingProductResponse::productId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AiMdGeminiOutfitResult.OutfitCandidate> savableOutfits = savableOutfits(
                aiResult,
                wardrobeById,
                productById
        );
        if (savableOutfits.size() < OUTFIT_COUNT) {
            throw new IllegalStateException("AI MD가 저장 가능한 코디 4개를 구성하지 못했습니다.");
        }

        /*
         * 추천 조회 단계에서는 DB에 아무것도 저장하지 않는다.
         * 사용자가 마음에 드는 코디를 선택하고 저장 버튼을 눌렀을 때만 saveRecommendedOutfit에서 저장한다.
         */
        List<OutfitRecommendation> outfitRecommendations = savableOutfits.stream()
                .map(candidate -> toOutfitRecommendation(persona, candidate, wardrobeById, productById))
                .toList();

        return new AiMdOutfitRecommendationResponse(persona.toResponse(), outfitRecommendations);
    }

    @Transactional
    public SavedOutfitRecommendation saveRecommendedOutfit(Long userId, String mdId, AiMdOutfitSaveRequest request) {
        User user = findUser(userId);
        AiMdPersona persona = resolvePersonaForUser(user, mdId);
        List<WardrobeClothes> wardrobeItems = findOwnedWardrobeItems(userId);
        Map<Long, WardrobeClothes> wardrobeById = wardrobeItems.stream()
                .collect(Collectors.toMap(WardrobeClothes::getId, Function.identity(), (left, right) -> left));

        List<WardrobeClothes> ownedItems = request.wardrobeClothesIds().stream()
                .map(wardrobeById::get)
                .filter(Objects::nonNull)
                .toList();
        if (ownedItems.isEmpty()) {
            throw new IllegalArgumentException("저장할 코디에는 보유 옷이 최소 1개 포함되어야 합니다.");
        }

        OutfitBook outfitBook = outfitBookRepository.findByUser_Id(userId)
                .orElseGet(() -> outfitBookRepository.save(OutfitBook.create(user)));
        List<NaverShoppingProductResponse> externalProducts = request.externalProducts() == null
                ? List.of()
                : request.externalProducts().stream()
                        .filter(Objects::nonNull)
                        .toList();
        if (!hasCompleteOutfitComposition(ownedItems, externalProducts)) {
            throw new IllegalArgumentException("저장할 코디에는 상의, 하의, 신발이 각각 최소 1개 포함되어야 합니다.");
        }
        return saveOutfitRecommendation(
                persona,
                outfitBook,
                request.title(),
                request.description(),
                request.situation(),
                request.season(),
                request.reason(),
                request.stylingTip(),
                ownedItems,
                externalProducts
        );
    }

    public AiMdProductRecommendationResponse recommendProducts(Long userId, String mdId) {
        User user = findUser(userId);
        AiMdPersona persona = resolvePersonaForUser(user, mdId);
        List<WardrobeClothes> wardrobeItems = findOwnedWardrobeItems(userId);
        String query = buildProductSearchQuery(persona, wardrobeItems);
        List<NaverShoppingProductResponse> products = naverApiService.searchShoppingProducts(query);

        AiMdGeminiProductResult aiResult = geminiService.generateJsonFromText(
                buildProductPrompt(persona, wardrobeItems, products),
                AiMdGeminiProductResult.class
        );

        Map<String, NaverShoppingProductResponse> productById = products.stream()
                .filter(product -> StringUtils.hasText(product.productId()))
                .collect(Collectors.toMap(NaverShoppingProductResponse::productId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<ProductRecommendation> recommendations = safeProductCandidates(aiResult).stream()
                .map(candidate -> toProductRecommendation(candidate, productById))
                .filter(Objects::nonNull)
                .limit(PRODUCT_RECOMMENDATION_COUNT)
                .collect(Collectors.toCollection(ArrayList::new));

        /*
         * Gemini가 후보 중 일부만 고르는 경우에도 프론트는 10개 영역을 안정적으로 렌더링할 수 있어야 한다.
         * 부족한 칸은 네이버 후보 순서대로 보충하되, 추천 사유는 MD 스타일 기반의 기본 문구로 명시한다.
         */
        for (NaverShoppingProductResponse product : products) {
            if (recommendations.size() >= PRODUCT_RECOMMENDATION_COUNT) {
                break;
            }
            boolean alreadyAdded = recommendations.stream()
                    .anyMatch(recommendation -> Objects.equals(recommendation.product().productId(), product.productId()));
            if (!alreadyAdded) {
                recommendations.add(new ProductRecommendation(product, persona.displayName() + " MD 스타일에 맞는 후보 상품입니다."));
            }
        }

        return new AiMdProductRecommendationResponse(persona.toResponse(), query, recommendations);
    }

    private SavedOutfitRecommendation saveOutfitRecommendation(
            AiMdPersona persona,
            OutfitBook outfitBook,
            String title,
            String description,
            String situation,
            String season,
            String reason,
            String stylingTip,
            List<WardrobeClothes> ownedItems,
            List<NaverShoppingProductResponse> externalProducts
    ) {
        /*
         * 이 메서드는 사용자가 저장 버튼을 누른 뒤에만 호출된다.
         * 추천 조회 단계에서는 Outfit/OutfitItem/외부 Clothes를 만들지 않아 사용자가 원하지 않는 코디가 저장되지 않는다.
         */
        if (ownedItems.isEmpty()) {
            throw new IllegalStateException("AI MD가 보유 옷을 포함하지 않은 코디를 반환했습니다.");
        }
        List<Clothes> externalClothes = externalProducts.stream()
                .map(product -> getOrCreateExternalClothes(persona, product))
                .toList();

        Outfit outfit = outfitRepository.save(Outfit.builder()
                .outfitBook(outfitBook)
                .title(defaultIfBlank(title, persona.displayName() + " MD 추천 코디"))
                .description(defaultIfBlank(description, defaultIfBlank(reason, defaultOutfitReason(persona))))
                .thumbnailUrl(resolveThumbnailUrl(ownedItems, externalProducts))
                .situation(defaultIfBlank(situation, "DAILY"))
                .season(defaultIfBlank(season, resolveSeason(ownedItems)))
                .favorite(false)
                .build());

        List<OutfitItem> outfitItems = new ArrayList<>();
        int layerOrder = 0;
        for (WardrobeClothes ownedItem : ownedItems) {
            outfitItems.add(toOutfitItem(outfit, ownedItem.getClothes(), layerOrder++));
        }
        for (Clothes externalItem : externalClothes) {
            outfitItems.add(toOutfitItem(outfit, externalItem, layerOrder++));
        }
        outfitItemRepository.saveAll(outfitItems);

        return new SavedOutfitRecommendation(
                OutfitResponse.from(outfit),
                defaultIfBlank(reason, defaultOutfitReason(persona)),
                defaultIfBlank(stylingTip, persona.speechStyle()),
                ownedItems.stream()
                        .map(item -> ClothesResponse.from(item.getClothes(), item))
                        .toList(),
                externalClothes.stream()
                        .map(ClothesResponse::from)
                        .toList()
        );
    }

    private OutfitRecommendation toOutfitRecommendation(
            AiMdPersona persona,
            AiMdGeminiOutfitResult.OutfitCandidate candidate,
            Map<Long, WardrobeClothes> wardrobeById,
            Map<String, NaverShoppingProductResponse> productById
    ) {
        List<WardrobeClothes> ownedItems = candidate.wardrobeClothesIds().stream()
                .map(wardrobeById::get)
                .filter(Objects::nonNull)
                .toList();
        if (ownedItems.isEmpty()) {
            throw new IllegalStateException("AI MD가 보유 옷을 포함하지 않은 코디를 반환했습니다.");
        }
        List<NaverShoppingProductResponse> externalProducts = candidate.externalProductIds().stream()
                .map(productById::get)
                .filter(Objects::nonNull)
                .toList();

        return new OutfitRecommendation(
                defaultIfBlank(candidate.title(), persona.displayName() + " MD 추천 코디"),
                defaultIfBlank(candidate.description(), candidate.reason()),
                defaultIfBlank(candidate.situation(), "DAILY"),
                defaultIfBlank(candidate.season(), resolveSeason(ownedItems)),
                defaultIfBlank(candidate.reason(), defaultOutfitReason(persona)),
                defaultIfBlank(candidate.stylingTip(), persona.speechStyle()),
                ownedItems.stream()
                        .map(item -> ClothesResponse.from(item.getClothes(), item))
                        .toList(),
                externalProducts
        );
    }

    private OutfitItem toOutfitItem(Outfit outfit, Clothes clothes, int layerOrder) {
        return OutfitItem.builder()
                .outfit(outfit)
                .clothes(clothes)
                .itemRole(defaultIfBlank(clothes.getCategory(), "ITEM"))
                .layerOrder(layerOrder)
                .build();
    }

    private Clothes getOrCreateExternalClothes(AiMdPersona persona, NaverShoppingProductResponse product) {
        if (StringUtils.hasText(product.productId())) {
            var existing = clothesRepository.findByExternalProductId(product.productId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        List<ClothesStyleDto> styleDtos = styleDtosFor(persona);
        Long clothesId = externalClothesService.getOrCreateExternalClothes(
                new NaverProductCreateRequest(
                        defaultIfBlank(product.productId(), "NAVER_" + Math.abs(defaultIfBlank(product.link(), product.title()).hashCode())),
                        product.brand(),
                        product.category3(),
                        product.title(),
                        product.image(),
                        product.link(),
                        "",
                        "",
                        List.of(new ClothingColorDto("WHITE", ColorRole.PRIMARY, (byte) 1)),
                        styleDtos
                ),
                List.of(new ClothingColorDto("WHITE", ColorRole.PRIMARY, (byte) 1)),
                styleDtos
        );
        return clothesRepository.findById(clothesId)
                .orElseThrow(() -> new EntityNotFoundException("외부 상품을 저장하지 못했습니다."));
    }

    private List<ClothesStyleDto> styleDtosFor(AiMdPersona persona) {
        Map<String, Style> styleByCode = styleRepository.findByCodeIn(persona.styleCodes()).stream()
                .collect(Collectors.toMap(Style::getCode, Function.identity()));
        List<ClothesStyleDto> styleDtos = new ArrayList<>();
        byte sortOrder = 1;
        for (String styleCode : persona.styleCodes()) {
            Style style = styleByCode.get(styleCode);
            if (style == null) {
                continue;
            }
            styleDtos.add(new ClothesStyleDto(
                    style.getId(),
                    sortOrder == 1 ? StyleRole.PRIMARY : StyleRole.SECONDARY,
                    sortOrder++
            ));
        }
        return styleDtos;
    }

    private ProductRecommendation toProductRecommendation(
            AiMdGeminiProductResult.ProductCandidate candidate,
            Map<String, NaverShoppingProductResponse> productById
    ) {
        NaverShoppingProductResponse product = productById.get(candidate.productId());
        if (product == null) {
            return null;
        }
        return new ProductRecommendation(product, defaultIfBlank(candidate.reason(), "AI MD가 옷장과 잘 맞는다고 판단한 상품입니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private AiMdPersona resolvePersonaForUser(User user, String mdId) {
        AiMdPersona persona = AiMdPersona.fromId(mdId);
        if (!persona.supports(user.getGender())) {
            throw new IllegalArgumentException("사용자 성별에 맞지 않는 AI MD입니다.");
        }
        return persona;
    }

    private List<WardrobeClothes> findOwnedWardrobeItems(Long userId) {
        List<WardrobeClothes> wardrobeItems = wardrobeClothesRepository.findOwnedForStatistics(userId, OwnershipStatus.OWNED);
        if (wardrobeItems.isEmpty()) {
            throw new IllegalStateException("AI MD 추천을 받으려면 보유 옷을 먼저 등록해 주세요.");
        }
        return wardrobeItems;
    }

    private List<AiMdGeminiOutfitResult.OutfitCandidate> savableOutfits(
            AiMdGeminiOutfitResult aiResult,
            Map<Long, WardrobeClothes> wardrobeById,
            Map<String, NaverShoppingProductResponse> productById
    ) {
        if (aiResult == null || aiResult.outfits() == null) {
            return List.of();
        }
        /*
         * Gemini가 반환한 wardrobeClothesId는 외부 입력이므로 raw id 존재 여부만 믿지 않는다.
         * 실제 현재 사용자 옷장에 매핑되는 보유 옷이 1개 이상 남고,
         * 보유 옷과 외부 상품을 합쳐 상의·하의·신발이 모두 구성된 후보만 반환 대상으로 확정한다.
         */
        return aiResult.outfits().stream()
                .filter(outfit -> {
                    List<WardrobeClothes> ownedItems = outfit.wardrobeClothesIds().stream()
                            .map(wardrobeById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    if (ownedItems.isEmpty()) {
                        return false;
                    }
                    List<NaverShoppingProductResponse> externalProducts = outfit.externalProductIds().stream()
                            .map(productById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return hasCompleteOutfitComposition(ownedItems, externalProducts);
                })
                .limit(OUTFIT_COUNT)
                .toList();
    }

    /**
     * 일반 검색 한 번으로는 후보가 상의에 치우칠 수 있으므로 코디 구성 카테고리별로 상품을 조회합니다.
     * 네이버 검색 결과 중 실제 판별 카테고리가 검색 목적과 일치하는 상품만 Gemini 후보로 전달합니다.
     */
    private List<NaverShoppingProductResponse> searchOutfitProductsByCategory(AiMdPersona persona) {
        String genderKeyword = persona.gender() == User.Gender.MALE ? "남성" : "여성";
        String styleKeyword = persona.styleNames().get(0);
        Map<String, NaverShoppingProductResponse> productsById = new LinkedHashMap<>();

        for (Map.Entry<String, String> categoryEntry : OUTFIT_CATEGORY_SEARCH_KEYWORDS.entrySet()) {
            String expectedCategory = categoryEntry.getKey();
            String query = Stream.of(genderKeyword, styleKeyword, categoryEntry.getValue())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(" "));

            naverApiService.searchShoppingProducts(query, OUTFIT_PRODUCTS_PER_CATEGORY, 1, "sim").stream()
                    .filter(product -> expectedCategory.equals(resolveExternalProductCategory(product)))
                    .filter(product -> StringUtils.hasText(product.productId()))
                    .forEach(product -> productsById.putIfAbsent(product.productId(), product));
        }
        return List.copyOf(productsById.values());
    }

    /**
     * 추천 조회와 저장 요청 모두 동일한 완성형 코디 규칙을 적용합니다.
     * 아우터는 선택 사항이지만 상의·하의·신발은 각각 최소 한 개가 필요합니다.
     */
    private boolean hasCompleteOutfitComposition(
            List<WardrobeClothes> ownedItems,
            List<NaverShoppingProductResponse> externalProducts
    ) {
        Set<String> categories = new HashSet<>();
        ownedItems.stream()
                .map(WardrobeClothes::getClothes)
                .map(Clothes::getCategory)
                .filter(StringUtils::hasText)
                .map(category -> category.trim().toUpperCase())
                .forEach(categories::add);
        externalProducts.stream()
                .map(this::resolveExternalProductCategory)
                .filter(StringUtils::hasText)
                .forEach(categories::add);
        return categories.containsAll(REQUIRED_OUTFIT_CATEGORIES);
    }

    /**
     * 네이버의 카테고리와 상품명을 함께 사용해 코디 구성 카테고리를 판별합니다.
     * 신발·하의·아우터를 상의보다 먼저 확인해 복합 상품명에서 상의로 잘못 분류되는 경우를 줄입니다.
     */
    private String resolveExternalProductCategory(NaverShoppingProductResponse product) {
        if (product == null) {
            return null;
        }
        String text = Stream.of(
                        product.category1(),
                        product.category2(),
                        product.category3(),
                        product.category4(),
                        product.title()
                )
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase().replaceAll("\\s+", ""))
                .collect(Collectors.joining(" "));

        if (containsAny(text, "신발", "구두", "슈즈", "스니커", "운동화", "로퍼", "더비", "부츠", "샌들", "슬리퍼", "힐", "플랫")) {
            return "SHOES";
        }
        if (containsAny(text, "바지", "팬츠", "슬랙스", "데님", "청바지", "스커트", "치마", "쇼츠", "반바지", "카고", "조거")) {
            return "BOTTOM";
        }
        if (containsAny(text, "패딩", "코트", "자켓", "재킷", "점퍼", "바람막이", "집업", "블루종", "블레이저", "무스탕", "베스트", "조끼", "야상", "아우터")) {
            return "OUTER";
        }
        if (containsAny(text, "셔츠", "티셔츠", "맨투맨", "후드", "니트", "스웨터", "가디건", "블라우스", "민소매", "카라", "폴로", "탑", "긴팔", "반팔")) {
            return "TOP";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        return Stream.of(keywords).anyMatch(text::contains);
    }

    private List<AiMdGeminiProductResult.ProductCandidate> safeProductCandidates(AiMdGeminiProductResult aiResult) {
        if (aiResult == null || aiResult.products() == null) {
            return List.of();
        }
        return aiResult.products();
    }

    private String buildProductSearchQuery(AiMdPersona persona, List<WardrobeClothes> wardrobeItems) {
        String genderKeyword = persona.gender() == User.Gender.MALE ? "남성" : "여성";
        String colorKeyword = wardrobeItems.stream()
                .map(WardrobeClothes::getClothes)
                .flatMap(clothes -> clothes.getSortedColorTags().stream())
                .min(Comparator.comparing(ClothingColor::getSortOrder))
                .map(ClothingColor::getColorCode)
                .map(this::toColorLabel)
                .orElse("");
        return Stream.of(genderKeyword, colorKeyword, persona.styleNames().get(0), "코디 아이템")
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(" "));
    }

    private String buildOutfitPrompt(
            AiMdPersona persona,
            List<WardrobeClothes> wardrobeItems,
            List<NaverShoppingProductResponse> externalProducts
    ) {
        return """
                당신은 옷장난감 서비스에서 고객의 옷장을 직접 살펴보고 코디를 제안하는 전문 패션 MD입니다.
                아래 MD의 취향과 화법을 자신의 정체성으로 유지하면서, 사용자의 보유 옷과 외부 상품 후보를 섞어 저장 가능한 완성형 코디를 정확히 4개 구성하세요.

                [MD]
                이름: %s
                스타일: %s
                말투: %s
                설명: %s
                추천 사유 화법: %s

                [보유 옷]
                %s

                [외부 상품 후보]
                %s

                규칙:
                - outfits 배열 길이는 반드시 4입니다.
                - 각 코디는 wardrobeClothesIds를 최소 1개 이상 포함해야 합니다.
                - 각 코디는 보유 옷과 외부 상품을 합쳐 TOP(상의), BOTTOM(하의), SHOES(신발)를 각각 최소 1개 포함해야 합니다.
                - OUTER(아우터)는 계절과 스타일에 맞을 때 추가하고, TOP은 이너와 레이어드 상의처럼 여러 개 선택할 수 있습니다.
                - 상의만 여러 개 조합한 결과는 코디로 인정하지 않습니다. 반드시 하의와 신발까지 완성합니다.
                - wardrobeClothesIds는 보유 옷 목록의 wardrobeClothesId만 사용합니다.
                - externalProductIds는 외부 상품 후보의 productId만 사용합니다.
                - 외부 상품은 필요할 때만 섞되, 코디 저장이 가능하도록 선택한 productId를 명확히 넣습니다.
                - reason은 사용자가 "왜 이 코디가 나에게 어울리는지" 바로 이해할 수 있도록 2~3개의 짧은 문장으로 작성하며, 전체 분량은 한글 기준 약 180~260자로 제한합니다.
                - reason에는 선택한 보유 옷과 외부 상품을 빠짐없이 한 번씩 언급합니다. 상품명이 길면 브랜드나 핵심 상품명으로 자연스럽게 줄여 씁니다.
                - 상의·하의·아우터·신발 등 각 아이템이 코디에서 맡는 역할을 색상, 핏, 소재, 실루엣 중 확인 가능한 특징과 연결해 짧게 설명합니다.
                - 아이템별 설명을 따로 나열하지 말고, "상의가 중심을 잡고 하의가 균형을 맞추며 신발이 마무리한다"처럼 코디 전체의 조합 이유로 자연스럽게 이어 씁니다.
                - reason은 %s MD가 사용자에게 직접 코디를 제안하는 말투로 작성하며, 페르소나의 스타일 취향과 추천 사유 화법을 일관되게 반영합니다.
                - reason에서 "AI", "인공지능", "모델", "데이터", "분석 결과", "알고리즘", "사용자님" 같은 기계적이거나 부자연스러운 표현을 사용하지 않습니다.
                - 사용자의 키, 체중, 체형, 신체 비율은 제공되지 않았으므로 "길어 보인다", "날씬해 보인다", "덩치가 좋아 보인다", "비율이 좋아진다"처럼 외형 변화를 단정하지 않습니다.
                - 상품명에 체형을 지칭하는 표현이 포함되어 있어도 추천 사유에는 옮겨 쓰지 않습니다.
                - 확인할 수 없는 직업, 일정, 취향을 추측하거나 모든 코디에 같은 상투적인 문장을 반복하지 않습니다.
                - description은 코디의 전체적인 분위기를 한 문장으로 요약하고, reason과 같은 내용을 그대로 반복하지 않습니다.
                - stylingTip은 소매를 걷는 방법, 신발·가방 선택, 핏 조절처럼 사용자가 바로 적용할 수 있는 팁을 한 문장으로 작성합니다.
                - title, description, reason, stylingTip에서 자신을 AI라고 소개하지 않고 실제 %s MD처럼 말합니다.
                - situation은 DAILY, DATE, WORK, TRAVEL 중 하나를 권장합니다.
                - season은 SPRING, SUMMER, FALL, WINTER 또는 ALL_SEASON 중 하나를 권장합니다.
                - JSON 외 문장은 쓰지 않습니다.

                응답 JSON:
                {
                  "outfits": [
                    {
                      "title": "string",
                      "description": "string",
                      "situation": "DAILY",
                      "season": "ALL_SEASON",
                      "reason": "string",
                      "stylingTip": "string",
                      "wardrobeClothesIds": [1],
                      "externalProductIds": ["123"]
                    }
                  ]
                }
                """.formatted(
                persona.displayName(),
                persona.styleNames(),
                persona.speechStyle(),
                persona.description(),
                persona.recommendationVoiceGuide(),
                summarizeWardrobeItems(wardrobeItems),
                summarizeProducts(externalProducts),
                persona.displayName(),
                persona.displayName()
        );
    }

    /**
     * Gemini가 추천 사유를 비워 반환한 예외 상황에서도 기계적인 공통 문구 대신
     * 선택한 MD의 정체성이 드러나는 최소한의 사용자 메시지를 제공합니다.
     */
    private String defaultOutfitReason(AiMdPersona persona) {
        return switch (persona) {
            case TAE_SIK -> "네 옷장에서 핏과 분위기가 자연스럽게 이어지는 조합으로 골랐어. "
                    + "힘은 별로 안 줬는데 옷 좀 입었다는 소리는 듣겠는데?";
            case JUN_SIK -> "가지고 계신 옷의 실루엣이 단정하게 이어지도록 구성했습니다. "
                    + "과한 장식 없이도 도시적인 인상이 완성되는 조합입니다.";
            case SE_SOON -> "옷장에 있는 아이템의 색과 실루엣이 차분하게 연결되도록 정리했어요. "
                    + "유행을 크게 타지 않으면서 깔끔하게 입기 좋은 조합입니다.";
            case GA_HYUN -> "가지고 계신 옷의 시크한 분위기는 살리고, 실루엣에 포인트가 생기도록 골랐어요. "
                    + "과하게 꾸미지 않아도 도회적인 무드가 분명한 조합이에요.";
            case SEONG_MI -> "옷장에 있는 아이템을 편하게 활용하면서도 흐트러져 보이지 않게 맞춰봤어요. "
                    + "자주 손이 가면서 은근히 센스 있어 보이는 조합이에요.";
        };
    }

    private String buildProductPrompt(
            AiMdPersona persona,
            List<WardrobeClothes> wardrobeItems,
            List<NaverShoppingProductResponse> products
    ) {
        return """
                당신은 옷장난감 서비스의 AI MD입니다.
                사용자의 옷장 분위기와 MD 스타일을 함께 고려해 외부 상품 후보 중 추천 상품 10개를 고르세요.

                [MD]
                이름: %s
                스타일: %s
                말투: %s
                설명: %s

                [사용자 옷장 요약]
                %s

                [상품 후보]
                %s

                규칙:
                - products 배열은 가능한 한 10개를 반환합니다.
                - productId는 상품 후보 목록에 있는 값만 사용합니다.
                - reason은 사용자 옷장과 MD 스타일을 함께 언급합니다.
                - JSON 외 문장은 쓰지 않습니다.

                응답 JSON:
                {
                  "products": [
                    {
                      "productId": "string",
                      "reason": "string"
                    }
                  ]
                }
                """.formatted(
                persona.displayName(),
                persona.styleNames(),
                persona.speechStyle(),
                persona.description(),
                summarizeWardrobeItems(wardrobeItems),
                summarizeProducts(products)
        );
    }

    private String summarizeWardrobeItems(List<WardrobeClothes> wardrobeItems) {
        return wardrobeItems.stream()
                .limit(MAX_WARDROBE_ITEMS_FOR_PROMPT)
                .map(item -> {
                    Clothes clothes = item.getClothes();
                    return "- wardrobeClothesId=%d, clothesId=%d, name=%s, category=%s, itemType=%s, color=%s, styles=%s, season=%s"
                            .formatted(
                                    item.getId(),
                                    clothes.getId(),
                                    clothes.getName(),
                                    clothes.getCategory(),
                                    toItemTypeLabel(clothes.getItemType()),
                                    primaryColorLabel(clothes),
                                    styleNames(clothes),
                                    defaultIfBlank(
                                            clothes.getSeason() != null ? clothes.getSeason().name() : null,
                                            "UNKNOWN"
                                    )
                            );
                })
                .collect(Collectors.joining("\n"));
    }

    private String summarizeProducts(List<NaverShoppingProductResponse> products) {
        if (products.isEmpty()) {
            return "- 없음";
        }
        return products.stream()
                .map(product -> "- productId=%s, outfitCategory=%s, title=%s, brand=%s, mall=%s, price=%s, naverCategory=%s/%s/%s"
                        .formatted(
                                product.productId(),
                                defaultIfBlank(resolveExternalProductCategory(product), "UNKNOWN"),
                                product.title(),
                                defaultIfBlank(product.brand(), "UNKNOWN"),
                                defaultIfBlank(product.mallName(), "UNKNOWN"),
                                product.lowestPrice(),
                                defaultIfBlank(product.category1(), ""),
                                defaultIfBlank(product.category2(), ""),
                                defaultIfBlank(product.category3(), "")
                        ))
                .collect(Collectors.joining("\n"));
    }

    private String resolveThumbnailUrl(List<WardrobeClothes> ownedItems, List<NaverShoppingProductResponse> externalProducts) {
        return ownedItems.stream()
                .map(WardrobeClothes::getUserImageUrl)
                .filter(StringUtils::hasText)
                .findFirst()
                .or(() -> externalProducts.stream().map(NaverShoppingProductResponse::image).filter(StringUtils::hasText).findFirst())
                .orElse("");
    }

    private String resolveSeason(List<WardrobeClothes> ownedItems) {
        return ownedItems.stream()
                .map(WardrobeClothes::getClothes)
                .map(clothes -> clothes.getSeason() != null ? clothes.getSeason().name() : null)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("ALL_SEASON");
    }

    private String primaryColorLabel(Clothes clothes) {
        return clothes.getSortedColorTags().stream()
                .filter(color -> color.getColorRole() == ColorRole.PRIMARY)
                .findFirst()
                .map(ClothingColor::getColorCode)
                .map(this::toColorLabel)
                .orElse("UNKNOWN");
    }

    private String styleNames(Clothes clothes) {
        return clothes.getSortedStyleTags().stream()
                .map(styleTag -> styleTag.getStyle().getName())
                .collect(Collectors.joining(", "));
    }

    private String toColorLabel(String colorCode) {
        try {
            return ClothesColor.fromCode(colorCode).getLabel();
        } catch (IllegalArgumentException exception) {
            return defaultIfBlank(colorCode, "UNKNOWN");
        }
    }

    private String toItemTypeLabel(String itemType) {
        try {
            return ClothesItemType.fromCode(itemType).getLabel();
        } catch (IllegalArgumentException exception) {
            return defaultIfBlank(itemType, "UNKNOWN");
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }
}
