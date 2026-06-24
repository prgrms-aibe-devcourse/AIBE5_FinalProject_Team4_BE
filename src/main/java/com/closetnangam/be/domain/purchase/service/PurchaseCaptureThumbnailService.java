package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.closetnangam.be.global.external.gemini.dto.GeminiThumbnailRegion;
import com.closetnangam.be.global.storage.ImageStorageService;
import com.closetnangam.be.global.storage.StoredImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseCaptureThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseCaptureThumbnailService.class);
    private static final int MIN_CROP_SIZE = 8;
    /** 이보다 작으면 확대 시 깨져 보이므로 크롭 대신 캡처 URL fallback을 사용합니다. */
    private static final int MIN_USEFUL_CROP_PX = 60;
    private static final double REGION_PADDING_RATIO = 0.12;

    static {
        // TwelveMonkeys ImageIO WebP 플러그인을 명시적으로 로드합니다.
        // SPI 자동 등록이 headless 서블릿 환경에서 지연될 수 있어 강제 스캔합니다.
        ImageIO.scanForPlugins();
    }

    private final ImageStorageService imageStorageService;

    public PurchaseCaptureThumbnailService(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    /**
     * 복수 상품 캡처에서 주문 행별 썸네일을 잘라 items[].imageUrl로 제공합니다.
     *
     * @param layoutItems AI가 추출한 전체 주문 행(반품 포함). 등록 가능 상품이 1개만 남아도 화면 행 위치 추정에 사용합니다.
     */
    public List<GeminiPurchaseCaptureItem> enrichWithItemThumbnails(
            Long userId,
            Long captureId,
            String captureImageUrl,
            String storedPath,
            List<GeminiPurchaseCaptureItem> items,
            List<GeminiPurchaseCaptureItem> layoutItems
    ) {
        if (items.isEmpty()) {
            return items;
        }
        // 단일 상품이라도 thumbnailRegion이 있으면 크롭을 시도합니다.
        // region도 없으면 캡처 원본 미리보기를 사용합니다.
        boolean singleItemWithoutRegion = items.size() == 1
                && (items.get(0).thumbnailRegion() == null || !items.get(0).thumbnailRegion().isValid());
        if (singleItemWithoutRegion) {
            return items;
        }

        byte[] sourceBytes = imageStorageService.readStoredImage(storedPath);
        BufferedImage sourceImage = readImage(sourceBytes);
        if (sourceImage == null) {
            log.warn("[구매내역AI] 캡처 원본을 읽지 못해 상품 썸네일을 생성하지 않습니다. captureId={}", captureId);
            return items;
        }

        int layoutRowCount = layoutItems == null || layoutItems.isEmpty() ? items.size() : layoutItems.size();
        // 첫 행을 기준점으로 잡고 행 간격을 일정하게 맞춰 행마다 좌표가 누적 드리프트되는 문제를 보정합니다.
        List<GeminiThumbnailRegion> calibratedRegions = calibrateRowRegions(items);
        List<GeminiPurchaseCaptureItem> enriched = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            int layoutRowIndex = resolveLayoutRowIndex(item, layoutItems, index);
            GeminiThumbnailRegion calibratedRegion = calibratedRegions == null ? null : calibratedRegions.get(index);
            String imageUrl = resolveItemSpecificImageUrl(
                    item,
                    captureImageUrl,
                    userId,
                    captureId,
                    index,
                    layoutRowIndex,
                    layoutRowCount,
                    sourceImage,
                    calibratedRegion
            );
            enriched.add(copyItem(item, imageUrl));
        }
        return enriched;
    }

    /**
     * 주문 행 썸네일 좌표를 보정합니다.
     *
     * <p>Gemini가 반환하는 행별 bounding box는 첫 행은 정확하지만 아래로 갈수록 누적 오차가 생기는 경향이 있습니다.
     * 첫 유효 행을 기준점(anchor)으로, 유효 행 간 간격의 중앙값을 pitch로 삼아 모든 행을 균일 간격으로 재배치합니다.
     * x 범위와 썸네일 높이는 기준 행 값을 재사용합니다.</p>
     *
     * @return items와 같은 길이로 정렬된 보정 영역. 유효한 기준 행이 하나도 없으면 {@code null}(기존 추정 경로 사용).
     */
    private static List<GeminiThumbnailRegion> calibrateRowRegions(List<GeminiPurchaseCaptureItem> items) {
        int n = items.size();
        double[] tops = new double[n];
        boolean[] valid = new boolean[n];
        double xmin = 0;
        double xmax = 0;
        double height = 0;
        int anchorIndex = -1;

        for (int i = 0; i < n; i++) {
            GeminiThumbnailRegion region = items.get(i).thumbnailRegion();
            if (region == null || !region.isValid()) {
                continue;
            }
            double top = normalizeCoordinate(region.ymin());
            double bottom = normalizeCoordinate(region.ymax());
            double left = normalizeCoordinate(region.xmin());
            double right = normalizeCoordinate(region.xmax());
            if (bottom <= top || right <= left) {
                continue;
            }
            tops[i] = top;
            valid[i] = true;
            if (anchorIndex < 0) {
                anchorIndex = i;
                xmin = left;
                xmax = right;
                height = bottom - top;
            }
        }

        if (anchorIndex < 0) {
            return null;
        }

        List<Double> deltas = new ArrayList<>();
        int previous = -1;
        for (int i = 0; i < n; i++) {
            if (!valid[i]) {
                continue;
            }
            if (previous >= 0) {
                double delta = (tops[i] - tops[previous]) / (i - previous);
                if (delta > 0) {
                    deltas.add(delta);
                }
            }
            previous = i;
        }

        double pitch;
        if (!deltas.isEmpty()) {
            deltas.sort(Double::compareTo);
            pitch = deltas.get(deltas.size() / 2);
        } else {
            // 기준 행만 유효한 경우: 썸네일 높이와 균등 분할 추정 중 작은 값으로 행 간격을 잡습니다.
            pitch = Math.min(0.86 / n, height * 1.8);
            if (pitch <= 0) {
                pitch = height;
            }
        }

        double anchorTop = tops[anchorIndex];
        List<GeminiThumbnailRegion> calibrated = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double top = anchorTop + pitch * (i - anchorIndex);
            top = Math.max(0.0, Math.min(top, 1.0 - height));
            calibrated.add(new GeminiThumbnailRegion(top, xmin, top + height, xmax));
        }
        return calibrated;
    }

    private String resolveItemSpecificImageUrl(
            GeminiPurchaseCaptureItem item,
            String captureImageUrl,
            Long userId,
            Long captureId,
            int itemIndex,
            int layoutRowIndex,
            int layoutRowCount,
            BufferedImage sourceImage,
            GeminiThumbnailRegion calibratedRegion
    ) {
        if (isDistinctItemImage(item.imageUrl(), captureImageUrl)) {
            return item.imageUrl().trim();
        }

        GeminiThumbnailRegion region = calibratedRegion != null ? calibratedRegion : item.thumbnailRegion();
        boolean estimated = false;
        if (region == null || !region.isValid()) {
            // 단일 상품은 추정 좌표로 크롭하지 않는다 (원본 캡처 URL을 대신 사용)
            if (layoutRowCount <= 1) {
                return null;
            }
            region = GeminiThumbnailRegion.estimateForOrderRow(layoutRowIndex, layoutRowCount);
            estimated = region != null;
        }
        if (region == null || !region.isValid()) {
            return null;
        }

        String croppedUrl = cropAndStore(userId, captureId, itemIndex, sourceImage, region);
        if (croppedUrl == null) {
            log.warn(
                    "[구매내역AI] 상품 썸네일 URL 생성 실패. captureId={}, itemIndex={}, estimated={}",
                    captureId,
                    itemIndex,
                    estimated
            );
        } else if (estimated) {
            log.info(
                    "[구매내역AI] AI 좌표 없음 — 추정 영역으로 썸네일 생성. captureId={}, itemIndex={}",
                    captureId,
                    itemIndex
            );
        }
        return croppedUrl;
    }

    private static int resolveLayoutRowIndex(
            GeminiPurchaseCaptureItem item,
            List<GeminiPurchaseCaptureItem> layoutItems,
            int fallbackIndex
    ) {
        if (layoutItems == null || layoutItems.isEmpty()) {
            return fallbackIndex;
        }
        for (int index = 0; index < layoutItems.size(); index++) {
            if (matchesLayoutRow(layoutItems.get(index), item)) {
                return index;
            }
        }
        return fallbackIndex;
    }

    private static boolean matchesLayoutRow(GeminiPurchaseCaptureItem layoutItem, GeminiPurchaseCaptureItem item) {
        if (layoutItem == item) {
            return true;
        }
        return StringUtils.hasText(layoutItem.name())
                && layoutItem.name().equals(item.name())
                && java.util.Objects.equals(
                normalize(layoutItem.optionText()),
                normalize(item.optionText())
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String cropAndStore(
            Long userId,
            Long captureId,
            int itemIndex,
            BufferedImage sourceImage,
            GeminiThumbnailRegion region
    ) {
        try {
            CropRectangle crop = padCropRectangle(
                    toPixelRectangle(region, sourceImage.getWidth(), sourceImage.getHeight()),
                    sourceImage.getWidth(),
                    sourceImage.getHeight()
            );
            if (crop.width() < MIN_CROP_SIZE || crop.height() < MIN_CROP_SIZE) {
                return null;
            }
            if (crop.width() < MIN_USEFUL_CROP_PX || crop.height() < MIN_USEFUL_CROP_PX) {
                log.info(
                        "[구매내역AI] 썸네일 영역이 너무 작아 크롭을 생략합니다. captureId={}, itemIndex={}, size={}x{}",
                        captureId,
                        itemIndex,
                        crop.width(),
                        crop.height()
                );
                return null;
            }
            BufferedImage cropped = sourceImage.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());
            BufferedImage rgb = toRgbImage(cropped);

            StoredImage storedImage = imageStorageService.storePurchaseCaptureThumbnail(
                    userId,
                    captureId,
                    itemIndex,
                    toJpegBytes(rgb)
            );
            return storedImage.publicUrl();
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "[구매내역AI] 상품 썸네일 크롭 실패. userId={}, captureId={}, itemIndex={}: {}",
                    userId,
                    captureId,
                    itemIndex,
                    exception.getMessage()
            );
            return null;
        }
    }

    static CropRectangle padCropRectangle(CropRectangle crop, int imageWidth, int imageHeight) {
        int padX = (int) Math.round(crop.width() * REGION_PADDING_RATIO);
        int padY = (int) Math.round(crop.height() * REGION_PADDING_RATIO);
        int x = clamp(crop.x() - padX, 0, Math.max(imageWidth - 1, 0));
        int y = clamp(crop.y() - padY, 0, Math.max(imageHeight - 1, 0));
        int right = clamp(crop.x() + crop.width() + padX, x + 1, imageWidth);
        int bottom = clamp(crop.y() + crop.height() + padY, y + 1, imageHeight);
        return new CropRectangle(x, y, right - x, bottom - y);
    }

    static CropRectangle toPixelRectangle(GeminiThumbnailRegion region, int imageWidth, int imageHeight) {
        double yMin = normalizeCoordinate(region.ymin());
        double xMin = normalizeCoordinate(region.xmin());
        double yMax = normalizeCoordinate(region.ymax());
        double xMax = normalizeCoordinate(region.xmax());

        int x = clamp((int) Math.round(xMin * imageWidth), 0, Math.max(imageWidth - 1, 0));
        int y = clamp((int) Math.round(yMin * imageHeight), 0, Math.max(imageHeight - 1, 0));
        int right = clamp((int) Math.round(xMax * imageWidth), x + 1, imageWidth);
        int bottom = clamp((int) Math.round(yMax * imageHeight), y + 1, imageHeight);

        return new CropRectangle(x, y, right - x, bottom - y);
    }

    private static double normalizeCoordinate(Double value) {
        if (value == null) {
            return 0.0;
        }
        if (value <= 1.0) {
            return value;
        }
        return value / 1000.0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static boolean isDistinctItemImage(String extractedImageUrl, String captureImageUrl) {
        if (!StringUtils.hasText(extractedImageUrl)) {
            return false;
        }
        if (!StringUtils.hasText(captureImageUrl)) {
            return true;
        }
        return !extractedImageUrl.trim().equals(captureImageUrl.trim());
    }

    private static BufferedImage readImage(byte[] sourceBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(sourceBytes));
        } catch (IOException exception) {
            return null;
        }
    }

    private static BufferedImage toRgbImage(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private static byte[] toJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    private static GeminiPurchaseCaptureItem copyItem(GeminiPurchaseCaptureItem item, String imageUrl) {
        return new GeminiPurchaseCaptureItem(
                item.name(),
                item.brandName(),
                item.category(),
                item.itemType(),
                item.primaryColor(),
                item.secondaryColors(),
                item.styles(),
                item.gender(),
                item.season(),
                item.optionText(),
                item.suggestedExternalSource(),
                imageUrl,
                item.thumbnailRegion(),
                item.orderStatus()
        );
    }

    record CropRectangle(int x, int y, int width, int height) {
    }
}
