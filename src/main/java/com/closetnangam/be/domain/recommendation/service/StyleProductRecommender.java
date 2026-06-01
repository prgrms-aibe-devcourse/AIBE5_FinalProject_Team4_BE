package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.clothes.repository.ClothesRepository;
import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
import com.closetnangam.be.global.external.clothes.dto.response.ProductDto;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleProductRecommender {

    private final ClothesRepository clothesRepository;
    private final ExternalClothesService externalClothesService;

    public List<RecommendResponse> recommendByStyle(Long wardrobeId) {
        List<String> topStyles = findTopStyles(wardrobeId);
        List<RecommendResponse> totalRecommendations = new ArrayList<>();

        if (topStyles.isEmpty()) {
            log.info("[추천 시스템] 선호 스타일 없음 - 기본 키워드 추천 작동");
            return convertToRecommendResponse(searchProductsWithFallback("기본", "트렌디한 반팔 셔츠"));
        }

        for (String styleName : topStyles) {
            String searchQuery = buildPrimarySearchQuery(styleName);

            log.info("[추천 시스템] 선호 스타일 기반 검색어 생성: {}", searchQuery);

            List<RecommendResponse> products = convertToRecommendResponse(searchProductsWithFallback(styleName, searchQuery));
            log.info("[추천 시스템] 검색어 '{}' 로 가져온 결과 개수: {}", searchQuery, products.size());
            totalRecommendations.addAll(products);
        }

        List<RecommendResponse> finalRecommendations = totalRecommendations.stream()
                .distinct()
                .limit(20)
                .collect(Collectors.toList());

        log.info("[Trace] StyleProductRecommender - 최종 추천 결과 개수: {}개", finalRecommendations.size());
        return finalRecommendations;
    }

    private List<ProductDto> searchProductsWithFallback(String styleName, String primaryQuery) {
        List<String> candidateQueries = buildCandidateQueries(styleName, primaryQuery);

        for (String candidateQuery : candidateQueries) {
            List<ProductDto> products = externalClothesService.searchProducts(candidateQuery);
            if (products != null && !products.isEmpty()) {
                if (!candidateQuery.equals(primaryQuery)) {
                    log.info(
                            "[추천 시스템] fallback 검색 성공: '{}' -> '{}' (style='{}')",
                            primaryQuery,
                            candidateQuery,
                            styleName
                    );
                }
                return products;
            }

            log.warn(
                    "[추천 시스템] 검색 결과 0건: query='{}', style='{}'",
                    candidateQuery,
                    styleName
            );
        }

        return List.of();
    }

    private List<String> buildCandidateQueries(String styleName, String primaryQuery) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, primaryQuery);
        addCandidate(candidates, stripBrandAndFillerWords(primaryQuery));
        addCandidate(candidates, stripBrandAndFillerWords(styleName));
        addCandidate(candidates, styleName);
        return new ArrayList<>(candidates);
    }

    private void addCandidate(Set<String> candidates, String query) {
        if (StringUtils.hasText(query)) {
            candidates.add(query.trim());
        }
    }

    private String buildPrimarySearchQuery(String styleName) {
        String safeStyleName = StringUtils.hasText(styleName) ? styleName.trim() : "기본";

        return switch (safeStyleName) {
            case "캐주얼" -> "지오다노 티셔츠";
            case "스트릿" -> "나이키 후드티";
            case "미니멀" -> "무신사스탠다드 슬랙스";
            default -> safeStyleName + " 브랜드 의류";
        };
    }

    private String stripBrandAndFillerWords(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }

        String normalized = query;
        String[] removableTokens = {
                "지오다노",
                "나이키",
                "무신사스탠다드",
                "브랜드",
                "의류",
                "패션",
                "추천",
                "코디",
                "트렌디한",
                "데일리",
                "남성",
                "여성",
                "남자",
                "여자",
                "신상",
                "룩"
        };

        for (String token : removableTokens) {
            normalized = normalized.replace(token, " ");
        }

        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private List<RecommendResponse> convertToRecommendResponse(List<ProductDto> products) {
        if (products == null || products.isEmpty()) {
            log.warn("[Trace] StyleProductRecommender - ProductDto 입력이 비어 있어 RecommendResponse 변환을 건너뜁니다.");
            return List.of();
        }

        log.info("[Trace] StyleProductRecommender - ProductDto 입력 개수: {}개", products.size());

        List<RecommendResponse> mapped = products.stream()
                .map(p -> new RecommendResponse(p.title(), p.link(), p.imageUrl(), p.lprice()))
                .collect(Collectors.toList());

        log.info("[Trace] StyleProductRecommender - RecommendResponse 변환 완료: {}개", mapped.size());
        return mapped;
    }

    private List<String> findTopStyles(Long wardrobeId) {
        return clothesRepository.countStyleTagsByWardrobe(wardrobeId, PageRequest.of(0, 3))
                .stream()
                .map(obj -> (String) obj[0])
                .collect(Collectors.toList());
    }
}
