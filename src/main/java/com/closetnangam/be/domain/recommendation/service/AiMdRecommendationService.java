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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        String searchQuery = buildProductSearchQuery(persona, wardrobeItems);
        List<NaverShoppingProductResponse> externalProducts = naverApiService.searchShoppingProducts(searchQuery);

        AiMdGeminiOutfitResult aiResult = geminiService.generateJsonFromText(
                buildOutfitPrompt(persona, wardrobeItems, externalProducts),
                AiMdGeminiOutfitResult.class
        );

        Map<Long, WardrobeClothes> wardrobeById = wardrobeItems.stream()
                .collect(Collectors.toMap(WardrobeClothes::getId, Function.identity(), (left, right) -> left));
        Map<String, NaverShoppingProductResponse> productById = externalProducts.stream()
                .filter(product -> StringUtils.hasText(product.productId()))
                .collect(Collectors.toMap(NaverShoppingProductResponse::productId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AiMdGeminiOutfitResult.OutfitCandidate> savableOutfits = savableOutfits(aiResult, wardrobeById);
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
                .description(defaultIfBlank(description, defaultIfBlank(reason, persona.description())))
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
                defaultIfBlank(reason, persona.description()),
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
                defaultIfBlank(candidate.reason(), persona.description()),
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
            Map<Long, WardrobeClothes> wardrobeById
    ) {
        if (aiResult == null || aiResult.outfits() == null) {
            return List.of();
        }
        /*
         * Gemini가 반환한 wardrobeClothesId는 외부 입력이므로 raw id 존재 여부만 믿지 않는다.
         * 실제 현재 사용자 옷장에 매핑되는 보유 옷이 1개 이상 남는 후보만 저장 가능 후보로 확정한다.
         */
        return aiResult.outfits().stream()
                .filter(outfit -> outfit.wardrobeClothesIds().stream().anyMatch(wardrobeById::containsKey))
                .limit(OUTFIT_COUNT)
                .toList();
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
                당신은 옷장난감 서비스의 AI MD입니다.
                사용자 성별에 맞는 MD 페르소나로, 사용자의 보유 옷과 외부 상품 후보를 섞어 저장 가능한 코디를 정확히 4개 구성하세요.

                [MD]
                이름: %s
                스타일: %s
                말투: %s
                설명: %s

                [보유 옷]
                %s

                [외부 상품 후보]
                %s

                규칙:
                - outfits 배열 길이는 반드시 4입니다.
                - 각 코디는 wardrobeClothesIds를 최소 1개 이상 포함해야 합니다.
                - wardrobeClothesIds는 보유 옷 목록의 wardrobeClothesId만 사용합니다.
                - externalProductIds는 외부 상품 후보의 productId만 사용합니다.
                - 외부 상품은 필요할 때만 섞되, 코디 저장이 가능하도록 선택한 productId를 명확히 넣습니다.
                - title, description, reason, stylingTip은 MD 말투를 반영합니다.
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
                summarizeWardrobeItems(wardrobeItems),
                summarizeProducts(externalProducts)
        );
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
                .map(product -> "- productId=%s, title=%s, brand=%s, mall=%s, price=%s, category=%s/%s/%s"
                        .formatted(
                                product.productId(),
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
