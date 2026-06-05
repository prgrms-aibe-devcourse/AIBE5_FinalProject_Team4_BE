package com.closetnangam.be.domain.purchase.service;

import com.closetnangam.be.global.config.StorageProperties;
import com.closetnangam.be.global.external.gemini.dto.GeminiPurchaseCaptureItem;
import com.closetnangam.be.global.external.gemini.dto.GeminiThumbnailRegion;
import com.closetnangam.be.global.storage.LocalImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseCaptureThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseCaptureThumbnailService.class);
    private static final int MIN_CROP_SIZE = 8;

    private final LocalImageStorageService localImageStorageService;
    private final StorageProperties storageProperties;

    public PurchaseCaptureThumbnailService(
            LocalImageStorageService localImageStorageService,
            StorageProperties storageProperties
    ) {
        this.localImageStorageService = localImageStorageService;
        this.storageProperties = storageProperties;
    }

    /**
     * 복수 상품 캡처에서 주문 행별 썸네일을 잘라 items[].imageUrl로 제공합니다.
     */
    public List<GeminiPurchaseCaptureItem> enrichWithItemThumbnails(
            Long userId,
            Long captureId,
            String captureImageUrl,
            String storedPath,
            List<GeminiPurchaseCaptureItem> items
    ) {
        if (items.size() <= 1) {
            return items;
        }

        byte[] sourceBytes = localImageStorageService.readStoredImage(storedPath);
        BufferedImage sourceImage = readImage(sourceBytes);
        if (sourceImage == null) {
            log.warn("[구매내역AI] 캡처 원본을 읽지 못해 상품 썸네일을 생성하지 않습니다. captureId={}", captureId);
            return items;
        }

        List<GeminiPurchaseCaptureItem> enriched = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            GeminiPurchaseCaptureItem item = items.get(index);
            String imageUrl = resolveItemSpecificImageUrl(
                    item,
                    captureImageUrl,
                    userId,
                    captureId,
                    index,
                    items.size(),
                    sourceImage
            );
            enriched.add(copyItem(item, imageUrl));
        }
        return enriched;
    }

    private String resolveItemSpecificImageUrl(
            GeminiPurchaseCaptureItem item,
            String captureImageUrl,
            Long userId,
            Long captureId,
            int itemIndex,
            int itemCount,
            BufferedImage sourceImage
    ) {
        if (isDistinctItemImage(item.imageUrl(), captureImageUrl)) {
            return item.imageUrl().trim();
        }

        GeminiThumbnailRegion region = item.thumbnailRegion();
        boolean estimated = false;
        if (region == null || !region.isValid()) {
            region = GeminiThumbnailRegion.estimateForOrderRow(itemIndex, itemCount);
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

    private String cropAndStore(
            Long userId,
            Long captureId,
            int itemIndex,
            BufferedImage sourceImage,
            GeminiThumbnailRegion region
    ) {
        try {
            CropRectangle crop = toPixelRectangle(region, sourceImage.getWidth(), sourceImage.getHeight());
            if (crop.width() < MIN_CROP_SIZE || crop.height() < MIN_CROP_SIZE) {
                return null;
            }
            BufferedImage cropped = sourceImage.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());
            BufferedImage rgb = toRgbImage(cropped);

            String fileName = captureId + "-item-" + itemIndex + ".jpg";
            Path targetDirectory = Paths.get(
                    storageProperties.getLocal().getBasePath(),
                    "purchase-captures",
                    String.valueOf(userId)
            );
            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(fileName);
            ImageIO.write(rgb, "jpg", targetPath.toFile());

            return storageProperties.getLocal().getBaseUrl()
                    + "/purchase-captures/" + userId + "/" + fileName;
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
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
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
                item.optionText(),
                item.suggestedExternalSource(),
                imageUrl,
                item.thumbnailRegion()
        );
    }

    record CropRectangle(int x, int y, int width, int height) {
    }
}
