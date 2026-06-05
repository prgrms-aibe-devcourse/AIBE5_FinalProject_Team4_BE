package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.global.external.gemini.dto.GeminiThumbnailRegion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseCaptureThumbnailServiceTest {

    @Test
    void toPixelRectangle_convertsThousandScaleCoordinates() {
        GeminiThumbnailRegion region = new GeminiThumbnailRegion(100.0, 50.0, 200.0, 150.0);

        PurchaseCaptureThumbnailService.CropRectangle crop =
                PurchaseCaptureThumbnailService.toPixelRectangle(region, 1000, 2000);

        assertThat(crop.x()).isEqualTo(50);
        assertThat(crop.y()).isEqualTo(200);
        assertThat(crop.width()).isEqualTo(100);
        assertThat(crop.height()).isEqualTo(200);
    }

    @Test
    void toPixelRectangle_convertsZeroToOneScaleCoordinates() {
        GeminiThumbnailRegion region = new GeminiThumbnailRegion(0.1, 0.05, 0.2, 0.15);

        PurchaseCaptureThumbnailService.CropRectangle crop =
                PurchaseCaptureThumbnailService.toPixelRectangle(region, 1000, 2000);

        assertThat(crop.x()).isEqualTo(50);
        assertThat(crop.y()).isEqualTo(200);
        assertThat(crop.width()).isEqualTo(100);
        assertThat(crop.height()).isEqualTo(200);
    }

    @Test
    void estimateForOrderRow_returnsDistinctRegionsPerItem() {
        GeminiThumbnailRegion first = GeminiThumbnailRegion.estimateForOrderRow(0, 3);
        GeminiThumbnailRegion second = GeminiThumbnailRegion.estimateForOrderRow(1, 3);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.ymin()).isGreaterThan(first.ymin());
    }
}
