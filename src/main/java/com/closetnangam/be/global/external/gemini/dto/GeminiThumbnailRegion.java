package com.closetnangam.be.global.external.gemini.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Gemini가 추정한 캡처 이미지 내 썸네일 영역.
 * 좌표는 0~1 비율 또는 0~1000 정규화 스케일을 사용합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiThumbnailRegion(
        @JsonAlias("top") Double ymin,
        @JsonAlias("left") Double xmin,
        @JsonAlias("bottom") Double ymax,
        @JsonAlias("right") Double xmax
) {

    public boolean isValid() {
        return ymin != null
                && xmin != null
                && ymax != null
                && xmax != null
                && ymax > ymin
                && xmax > xmin;
    }

    /** 주문 행 개수 기반 기본 썸네일 위치(무신사 주문내역형 레이아웃 가정). */
    public static GeminiThumbnailRegion estimateForOrderRow(int itemIndex, int itemCount) {
        if (itemCount <= 0 || itemIndex < 0 || itemIndex >= itemCount) {
            return null;
        }
        double topMargin = 0.11;
        double usableHeight = 0.86;
        double rowHeight = usableHeight / itemCount;
        double rowTop = topMargin + rowHeight * itemIndex;
        double rowBottom = rowTop + rowHeight;
        double thumbSize = Math.min(0.14, rowHeight * 0.75);
        double xMin = 0.035;
        double yCenter = (rowTop + rowBottom) / 2.0;
        double yMin = yCenter - thumbSize / 2.0;
        double yMax = yCenter + thumbSize / 2.0;
        return new GeminiThumbnailRegion(yMin, xMin, yMax, xMin + thumbSize);
    }
}
