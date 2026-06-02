package com.closetnangam.be.domain.catalog.controller;

import com.closetnangam.be.domain.catalog.dto.response.CategoryCatalogResponse;
import com.closetnangam.be.domain.catalog.dto.response.CategoryUsageGuideResponse;
import com.closetnangam.be.domain.catalog.dto.response.ExternalSourcesResponse;
import com.closetnangam.be.domain.catalog.service.CategoryCatalogService;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Category", description = "공용 카테고리 API")
@SecurityRequirements
@RestController
@RequestMapping({"/api/v1/categories", "/api/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryCatalogService categoryCatalogService;

    @Operation(
            summary = "카테고리 전체 목록 조회",
            description = """
                    의류 category/item_type, styles, colors 공용 목록과 사용 설명서(guide)를 함께 반환합니다.
                    - guide: FE/BE 연동 규칙, 필드 설명, 예시
                    - categories: 대분류 + item_type 목록
                    - styles: 스타일 목록 (DB styles 테이블)
                    - colors: 컬러 code, name, hex
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryCatalogResponse>> getCatalog() {
        return ResponseEntity.ok(ApiResponse.ok(categoryCatalogService.getCatalog()));
    }

    @Operation(
            summary = "카테고리 사용 설명서 조회",
            description = "카테고리 API 연동 규칙과 저장/표시 예시만 별도로 조회합니다."
    )
    @GetMapping("/guide")
    public ResponseEntity<ApiResponse<CategoryUsageGuideResponse>> getUsageGuide() {
        return ResponseEntity.ok(ApiResponse.ok(categoryCatalogService.getUsageGuide()));
    }

    @Operation(summary = "AI 분류 가이드 조회", description = "Gemini 등 AI가 옷 분류 시 사용할 코드 목록")
    @GetMapping("/ai-guide")
    public ResponseEntity<ApiResponse<String>> getAiGuide() {
        return ResponseEntity.ok(ApiResponse.ok(categoryCatalogService.getAiClassificationGuide()));
    }

    @Operation(
            summary = "외부 쇼핑 출처 목록 조회",
            description = """
                    미보유 옷 저장 시 선택할 외부 출처 12개 + 직접입력 옵션을 반환합니다.
                    - 선택 항목: externalSource에 code 저장 (예: NAVER_SHOPPING, MUSINSA)
                    - 직접입력: allowsCustomInput=true 항목 선택 후 사용자 입력값을 externalSource에 저장
                    """
    )
    @GetMapping("/external-sources")
    public ResponseEntity<ApiResponse<ExternalSourcesResponse>> getExternalSources() {
        return ResponseEntity.ok(ApiResponse.ok(categoryCatalogService.getExternalSources()));
    }
}
