package com.closetnangam.be.domain.outfit.dto.response;

import com.closetnangam.be.domain.clothes.dto.response.ClothesResponse;
import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.outfit.entity.OutfitItem;

/**
 * 저장된 코디를 다시 조회할 때 실제 구성 옷을 복원하기 위한 응답 DTO.
 *
 * 보유 옷은 WardrobeClothes 정보를 함께 담아 wardrobeClothesId/size/season 등을 내려주고,
 * 외부 쇼핑 상품은 사용자 옷장 연결이 없으므로 Clothes 마스터 정보만 내려준다.
 */
public record OutfitItemResponse(
        Long outfitItemId,
        String itemRole,
        Integer layerOrder,
        ClothesResponse clothes
) {

    public static OutfitItemResponse from(OutfitItem outfitItem, WardrobeClothes wardrobeClothes) {
        return new OutfitItemResponse(
                outfitItem.getId(),
                outfitItem.getItemRole(),
                outfitItem.getLayerOrder(),
                ClothesResponse.from(outfitItem.getClothes(), wardrobeClothes)
        );
    }
}
