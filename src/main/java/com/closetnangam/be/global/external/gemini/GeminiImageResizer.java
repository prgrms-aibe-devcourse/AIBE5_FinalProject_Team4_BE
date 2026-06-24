package com.closetnangam.be.global.external.gemini;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Gemini API 호출 전 의류 단품 이미지를 축소해 토큰 사용량과 전송 크기를 줄입니다.
 *
 * <p>구매내역 캡처(긴 세로 스크린샷)에는 적용하지 않습니다.
 * 캡처를 리사이즈하면 가로폭이 300px대로 압축돼 OCR 품질이 크게 떨어집니다.</p>
 */
public class GeminiImageResizer {

    private static final Logger log = LoggerFactory.getLogger(GeminiImageResizer.class);
    private static final int MAX_DIMENSION = 1024;
    private static final float JPEG_QUALITY = 0.85f;

    private GeminiImageResizer() {
    }

    /**
     * 이미지를 MAX_DIMENSION 이하로 축소합니다.
     * 이미 충분히 작으면 원본 바이트를 그대로 반환합니다.
     *
     * @param imageBytes  원본 이미지 바이트
     * @param contentType 원본 Content-Type (image/jpeg, image/png, image/webp)
     * @return 리사이즈된 JPEG 바이트 (또는 리사이즈 불필요 시 원본)
     */
    public static ResizedImage resize(byte[] imageBytes, String contentType) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                return new ResizedImage(imageBytes, contentType);
            }

            int width = original.getWidth();
            int height = original.getHeight();

            if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
                return new ResizedImage(imageBytes, contentType);
            }

            double scale = (double) MAX_DIMENSION / Math.max(width, height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            byte[] resizedBytes = encodeJpeg(resized);
            log.debug("이미지 리사이즈: {}x{} → {}x{}, {}KB → {}KB",
                    width, height, newWidth, newHeight,
                    imageBytes.length / 1024, resizedBytes.length / 1024);

            return new ResizedImage(resizedBytes, "image/jpeg");
        } catch (Exception e) {
            log.warn("이미지 리사이즈 실패, 원본 사용: {}", e.getMessage());
            return new ResizedImage(imageBytes, contentType);
        }
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }

    public record ResizedImage(byte[] bytes, String contentType) {
    }
}
