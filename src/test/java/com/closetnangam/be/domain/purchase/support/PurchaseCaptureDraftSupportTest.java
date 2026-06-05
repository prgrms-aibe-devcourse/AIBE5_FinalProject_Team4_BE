package com.closetnangam.be.domain.purchase.support;

import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureExtractionResult;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseCaptureDraftSupportTest {

    @Test
    void resolveItems_usesFlatFieldsForSingleProduct() {
        GeminiPurchaseCaptureExtractionResult result = new GeminiPurchaseCaptureExtractionResult(
                "티셔츠",
                "BRAND",
                "TOP",
                "SHORT_SLEEVE",
                "WHITE",
                List.of(),
                List.of("CASUAL"),
                "M",
                "MUSINSA",
                "https://cdn.example.com/tee.jpg",
                null,
                "구매 확정",
                null
        );

        List<GeminiPurchaseCaptureItem> items = PurchaseCaptureDraftSupport.resolveItems(result);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).name()).isEqualTo("티셔츠");
    }

    @Test
    void resolveItems_prefersItemsArrayForMultipleProducts() {
        GeminiPurchaseCaptureItem first = new GeminiPurchaseCaptureItem(
                "티셔츠", "A", "TOP", "SHORT_SLEEVE", "WHITE", List.of(), List.of("CASUAL"), "M", "MUSINSA",
                "https://cdn.example.com/tee.jpg", null, "구매 확정"
        );
        GeminiPurchaseCaptureItem second = new GeminiPurchaseCaptureItem(
                "팬츠", "B", "BOTTOM", "JEANS", "BLUE", List.of(), List.of("CASUAL"), "32", "MUSINSA",
                "https://cdn.example.com/jeans.jpg", null, "구매 확정"
        );
        GeminiPurchaseCaptureExtractionResult result = new GeminiPurchaseCaptureExtractionResult(
                null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(first, second)
        );

        List<GeminiPurchaseCaptureItem> items = PurchaseCaptureDraftSupport.resolveItems(result);

        assertThat(items).hasSize(2);
        assertThat(items.get(1).name()).isEqualTo("팬츠");
    }

    @Test
    void resolveItemImageUrl_returnsOnlyItemSpecificThumbnail() {
        assertThat(PurchaseCaptureDraftSupport.resolveItemImageUrl(
                "https://cdn.example.com/tee.jpg",
                "http://localhost:8080/capture.jpg"
        )).isEqualTo("https://cdn.example.com/tee.jpg");

        assertThat(PurchaseCaptureDraftSupport.resolveItemImageUrl(
                null,
                "http://localhost:8080/capture.jpg"
        )).isNull();

        assertThat(PurchaseCaptureDraftSupport.resolveItemImageUrl(
                "http://localhost:8080/capture.jpg",
                "http://localhost:8080/capture.jpg"
        )).isNull();
    }

    @Test
    void normalizeExtractionItems_defaultsBrandAndStyle() {
        GeminiPurchaseCaptureItem item = new GeminiPurchaseCaptureItem(
                "슬랙스", null, "BOTTOM", "SLACKS", "BLACK", List.of(), List.of(), "36", "MUSINSA",
                null, null, "구매 확정"
        );

        List<GeminiPurchaseCaptureItem> normalized = PurchaseCaptureDraftSupport.normalizeExtractionItems(List.of(item));

        assertThat(normalized.get(0).brandName()).isEqualTo("UNKNOWN");
        assertThat(normalized.get(0).styles()).containsExactly("CASUAL");
    }

    @Test
    void filterRegistrableItems_excludesReturnOrders() {
        GeminiPurchaseCaptureItem returned = new GeminiPurchaseCaptureItem(
                "데님", "A", "BOTTOM", "JEANS", "BLACK", List.of(), List.of("CASUAL"), "38", "MUSINSA",
                null, null, "반품 완료"
        );
        GeminiPurchaseCaptureItem owned = new GeminiPurchaseCaptureItem(
                "슬랙스", "B", "BOTTOM", "SLACKS", "BLACK", List.of(), List.of("CASUAL"), "36", "MUSINSA",
                null, null, "구매 확정"
        );

        List<GeminiPurchaseCaptureItem> filtered = PurchaseCaptureDraftSupport.filterRegistrableItems(
                List.of(returned, returned, owned)
        );

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).name()).isEqualTo("슬랙스");
    }

    @Test
    void resolveItemIndex_rejectsOutOfRange() {
        assertThatThrownBy(() -> PurchaseCaptureDraftSupport.resolveItemIndex(2, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemIndex");
    }
}
