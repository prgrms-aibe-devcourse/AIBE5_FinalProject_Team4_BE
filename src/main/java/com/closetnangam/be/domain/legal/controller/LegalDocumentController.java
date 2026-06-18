package com.closetnangam.be.domain.legal.controller;

import com.closetnangam.be.domain.legal.dto.response.LegalDocumentResponse;
import com.closetnangam.be.domain.legal.service.LegalDocumentService;
import com.closetnangam.be.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Legal", description = "약관 문서 API")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @Operation(
            summary = "약관 문서 조회",
            description = """
                    BE docs/legal 최신 약관 markdown 원문을 조회합니다.
                    documentType: terms, privacy-policy, marketing-consent
                    """
    )
    @GetMapping("/{documentType}")
    public ResponseEntity<ApiResponse<LegalDocumentResponse>> getDocument(
            @PathVariable String documentType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(legalDocumentService.getDocument(documentType)));
    }
}
