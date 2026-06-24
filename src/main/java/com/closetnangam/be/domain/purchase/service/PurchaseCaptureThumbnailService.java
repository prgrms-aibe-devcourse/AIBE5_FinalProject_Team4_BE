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
        // 각 등록 대상 상품이 화면에서 차지하는 실제 주문 행 위치(반품/취소 행 포함)를 먼저 구합니다.
        int[] layoutRowIndices = new int[items.size()];
        for (int index = 0; index < items.size(); index++) {
            layoutRowIndices[index] = resolveLayoutRowIndex(items.get(index), layoutItems, index);
        }
        // 1순위: 실제 이미지를 스캔해 썸네일 밴드를 기하학적으로 검출(좌표 드리프트에 영향받지 않음).
        // 2순위: 검출에 실패하면 Gemini 좌표 기반 행 보정으로 fallback.
        List<GeminiThumbnailRegion> calibratedRegions = detectRegionsByImage(
                items,
                layoutRowIndices,
                layoutRowCount,
                sourceImage,
                captureId
        );
        if (calibratedRegions == null) {
            calibratedRegions = calibrateRowRegions(items, layoutRowIndices);
        }
        List<GeminiPurchaseCaptureItem> enriched = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            int layoutRowIndex = layoutRowIndices[index];
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
     * 캡처 이미지를 직접 분석해 주문 행 썸네일 영역을 검출합니다.
     *
     * <p>Gemini의 y좌표는 행 간격을 압축해 보고하는 등 신뢰도가 낮아, 첫 행(anchor)이 알려주는 썸네일 좌측 열만
     * 신뢰합니다. 그 열을 따라 세로로 스캔하면 썸네일(픽셀 변화량 큼)과 행 사이 여백(균일)이 구분되므로,
     * 연속된 고변화 구간을 썸네일 밴드로 묶습니다. 라이트/다크 테마 모두에서 동작합니다.</p>
     *
     * <p>검출된 밴드 수가 실제 화면 행 수({@code layoutRowCount})와 정확히 일치할 때만 사용하고,
     * 그렇지 않으면 {@code null}을 반환해 Gemini 좌표 기반 보정으로 fallback합니다.</p>
     *
     * @return items와 같은 길이의 보정 영역, 또는 검출 실패 시 {@code null}
     */
    private static List<GeminiThumbnailRegion> detectRegionsByImage(
            List<GeminiPurchaseCaptureItem> items,
            int[] layoutRowIndices,
            int layoutRowCount,
            BufferedImage sourceImage,
            Long captureId
    ) {
        if (layoutRowCount <= 1) {
            return null;
        }
        GeminiThumbnailRegion anchor = findAnchorRegion(items);
        if (anchor == null) {
            return null;
        }
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int xStart = clamp((int) Math.round(normalizeCoordinate(anchor.xmin()) * width), 0, Math.max(width - 1, 0));
        int xEnd = clamp((int) Math.round(normalizeCoordinate(anchor.xmax()) * width), xStart + 1, width);

        List<int[]> bands = detectThumbnailBands(sourceImage, xStart, xEnd);
        if (bands.size() != layoutRowCount) {
            log.info(
                    "[구매내역AI] 썸네일 밴드 검출 수 불일치 — 좌표 보정으로 fallback. captureId={}, bands={}, rows={}",
                    captureId,
                    bands.size(),
                    layoutRowCount
            );
            return null;
        }

        double xminN = (double) xStart / width;
        double xmaxN = (double) xEnd / width;
        List<GeminiThumbnailRegion> regions = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            int row = layoutRowIndices[i];
            if (row < 0 || row >= bands.size()) {
                return null;
            }
            int[] band = bands.get(row);
            double yminN = (double) band[0] / height;
            double ymaxN = (double) band[1] / height;
            regions.add(new GeminiThumbnailRegion(yminN, xminN, ymaxN, xmaxN));
        }
        log.info("[구매내역AI] 이미지 기반 썸네일 밴드 검출 성공. captureId={}, rows={}", captureId, layoutRowCount);
        return regions;
    }

    private static GeminiThumbnailRegion findAnchorRegion(List<GeminiPurchaseCaptureItem> items) {
        for (GeminiPurchaseCaptureItem item : items) {
            GeminiThumbnailRegion region = item.thumbnailRegion();
            if (region != null && region.isValid()) {
                double top = normalizeCoordinate(region.ymin());
                double bottom = normalizeCoordinate(region.ymax());
                double left = normalizeCoordinate(region.xmin());
                double right = normalizeCoordinate(region.xmax());
                if (bottom > top && right > left) {
                    return region;
                }
            }
        }
        return null;
    }

    /**
     * 썸네일 좌측 열을 세로로 스캔해 행별 변화량(휘도 표준편차)을 구하고, 고변화 구간을 썸네일 밴드로 묶습니다.
     *
     * @return [yTopPx, yBottomPx] 밴드 목록(위→아래 순)
     */
    private static List<int[]> detectThumbnailBands(BufferedImage image, int xStart, int xEnd) {
        int height = image.getHeight();
        int stripWidth = xEnd - xStart;
        int step = Math.max(1, stripWidth / 40);

        double[] rowScore = new double[height];
        double maxScore = 0;
        for (int y = 0; y < height; y++) {
            double sum = 0;
            double sumSq = 0;
            int count = 0;
            for (int x = xStart; x < xEnd; x += step) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                double lum = 0.299 * r + 0.587 * g + 0.114 * b;
                sum += lum;
                sumSq += lum * lum;
                count++;
            }
            if (count == 0) {
                continue;
            }
            double mean = sum / count;
            double variance = sumSq / count - mean * mean;
            double stdev = variance <= 0 ? 0 : Math.sqrt(variance);
            rowScore[y] = stdev;
            maxScore = Math.max(maxScore, stdev);
        }

        if (maxScore <= 0) {
            return List.of();
        }

        double threshold = Math.max(8.0, maxScore * 0.18);
        int maxGap = Math.max(2, height / 200);
        int minBandHeight = Math.max(MIN_USEFUL_CROP_PX, stripWidth / 2);

        List<int[]> bands = new ArrayList<>();
        int bandStart = -1;
        int bandEnd = -1;
        int gap = 0;
        for (int y = 0; y < height; y++) {
            boolean content = rowScore[y] >= threshold;
            if (content) {
                if (bandStart < 0) {
                    bandStart = y;
                }
                bandEnd = y;
                gap = 0;
            } else if (bandStart >= 0) {
                gap++;
                if (gap > maxGap) {
                    if (bandEnd - bandStart >= minBandHeight) {
                        bands.add(new int[]{bandStart, bandEnd});
                    }
                    bandStart = -1;
                    bandEnd = -1;
                    gap = 0;
                }
            }
        }
        if (bandStart >= 0 && bandEnd - bandStart >= minBandHeight) {
            bands.add(new int[]{bandStart, bandEnd});
        }
        return bands;
    }

    /**
     * 주문 행 썸네일 좌표를 보정합니다.
     *
     * <p>Gemini가 반환하는 행별 bounding box는 첫 행은 정확하지만 아래로 갈수록 누적 오차가 생기는 경향이 있습니다.
     * 첫 유효 행을 기준점(anchor)으로, 유효 행 간 간격의 중앙값을 pitch로 삼아 모든 행을 균일 간격으로 재배치합니다.
     * x 범위와 썸네일 높이는 기준 행 값을 재사용합니다.</p>
     *
     * <p>좌표축은 등록 대상 목록의 인덱스가 아니라 {@code layoutRowIndices}(반품/취소 행을 포함한 실제 화면 행 위치)를
     * 기준으로 한다. 중간에 등록 불가 행이 빠져 있어도 뒤 상품 좌표가 한 행씩 밀리지 않도록 하기 위함이다.
     * 화면 행 위치를 신뢰할 수 없으면(유효 행의 행 인덱스가 단조 증가하지 않으면) 보정을 포기하고 기존 경로로 fallback한다.</p>
     *
     * @param layoutRowIndices items와 같은 길이의 실제 화면 행 위치 배열
     * @return items와 같은 길이로 정렬된 보정 영역. 유효한 기준 행이 없거나 행 위치를 신뢰할 수 없으면 {@code null}(기존 추정 경로 사용).
     */
    private static List<GeminiThumbnailRegion> calibrateRowRegions(
            List<GeminiPurchaseCaptureItem> items,
            int[] layoutRowIndices
    ) {
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

        // 유효 행의 화면 행 위치가 단조 증가하지 않으면(행 매핑을 신뢰할 수 없으면) 보정을 포기한다.
        int previousRow = -1;
        for (int i = 0; i < n; i++) {
            if (!valid[i]) {
                continue;
            }
            if (layoutRowIndices[i] <= previousRow) {
                return null;
            }
            previousRow = layoutRowIndices[i];
        }

        List<Double> deltas = new ArrayList<>();
        int previous = -1;
        for (int i = 0; i < n; i++) {
            if (!valid[i]) {
                continue;
            }
            if (previous >= 0) {
                int rowSpan = layoutRowIndices[i] - layoutRowIndices[previous];
                double delta = (tops[i] - tops[previous]) / rowSpan;
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
            // 기준 행만 유효한 경우: 화면 전체 행 수 기준 균등 분할과 썸네일 높이 추정 중 작은 값으로 행 간격을 잡습니다.
            int rowCount = 1;
            for (int i = 0; i < n; i++) {
                rowCount = Math.max(rowCount, layoutRowIndices[i] + 1);
            }
            pitch = Math.min(0.86 / rowCount, height * 1.8);
            if (pitch <= 0) {
                pitch = height;
            }
        }

        double anchorTop = tops[anchorIndex];
        int anchorRow = layoutRowIndices[anchorIndex];
        List<GeminiThumbnailRegion> calibrated = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double top = anchorTop + pitch * (layoutRowIndices[i] - anchorRow);
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
