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
                "https://cdn.example.com/tee.jpg", null
        );
        GeminiPurchaseCaptureItem second = new GeminiPurchaseCaptureItem(
                "팬츠", "B", "BOTTOM", "JEANS", "BLUE", List.of(), List.of("CASUAL"), "32", "MUSINSA",
                "https://cdn.example.com/jeans.jpg", null
        );
        GeminiPurchaseCaptureExtractionResult result = new GeminiPurchaseCaptureExtractionResult(
                null, null, null, null, null, null, null, null, null, null, null,
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
    void resolveItemIndex_rejectsOutOfRange() {
        assertThatThrownBy(() -> PurchaseCaptureDraftSupport.resolveItemIndex(2, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemIndex");
    }
}
