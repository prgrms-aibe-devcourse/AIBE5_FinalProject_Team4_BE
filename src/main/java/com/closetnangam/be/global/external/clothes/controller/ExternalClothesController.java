package com.closetnangam.be.global.external.clothes.controller;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import com.closetnangam.be.global.external.clothes.dto.NaverItemRequest;
import com.closetnangam.be.global.external.clothes.dto.request.ClothesStyleDto;
import com.closetnangam.be.global.external.clothes.dto.request.ClothingColorDto;
import com.closetnangam.be.global.external.clothes.service.ExternalClothesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/external/clothes")
@RequiredArgsConstructor
public class ExternalClothesController {

    private final ExternalClothesService externalClothesService;
    private final WardrobeService wardrobeService;

    @PostMapping("/naver")
    public ResponseEntity<String> saveNaverProduct(
            @RequestParam Long userId,
            @RequestBody NaverItemRequest request,
            @RequestParam(required = false) List<ClothingColorDto> colorDtos, // 파라미터나 DTO 구조에 맞게 수집
            @RequestParam(required = false) List<ClothesStyleDto> styleDtos
    ) {
        Wardrobe wardrobe = wardrobeService.getOrCreateWardrobe(userId);

        Long clothesId = externalClothesService.saveNaverToWishlist(userId, wardrobe, request, colorDtos, styleDtos);

        return ResponseEntity.ok("External clothes saved. ID: " + clothesId);
    }
}
