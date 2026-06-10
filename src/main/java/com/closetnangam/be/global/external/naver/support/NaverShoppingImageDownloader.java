package com.closetnangam.be.global.external.naver.support;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * 네이버 쇼핑 상품 이미지 다운로드.
 * CDN이 404를 반환하면서 본문에 이미지 바이트를 내려주는 경우도 처리한다.
 */
public final class NaverShoppingImageDownloader {

    private NaverShoppingImageDownloader() {
    }

    public record DownloadedImage(byte[] bytes, String mimeType) {
    }

    public static Optional<DownloadedImage> tryDownload(RestTemplate restTemplate, String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return Optional.empty();
        }

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    byte[].class
            );
            return toDownloadedImage(imageUrl, response.getBody(), response.getHeaders());
        } catch (HttpStatusCodeException exception) {
            return toDownloadedImage(imageUrl, exception.getResponseBodyAsByteArray(), exception.getResponseHeaders());
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private static Optional<DownloadedImage> toDownloadedImage(
            String imageUrl,
            byte[] imageBytes,
            HttpHeaders headers
    ) {
        if (imageBytes == null || imageBytes.length == 0 || !looksLikeImage(imageBytes)) {
            return Optional.empty();
        }
        return Optional.of(new DownloadedImage(imageBytes, resolveMimeType(imageUrl, headers, imageBytes)));
    }

    private static HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; ClosetNangam/1.0)");
        headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        headers.set(HttpHeaders.REFERER, "https://shopping.naver.com/");
        return headers;
    }

    static boolean looksLikeImage(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return true;
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return true;
        }
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static String resolveMimeType(String imageUrl, HttpHeaders headers, byte[] imageBytes) {
        MediaType contentType = headers != null ? headers.getContentType() : null;
        if (contentType != null && "image".equals(contentType.getType())) {
            return contentType.toString();
        }
        if (imageBytes[0] == (byte) 0x89) {
            return "image/png";
        }
        if (imageBytes[0] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (imageBytes[0] == 'G') {
            return "image/gif";
        }
        if (imageBytes.length >= 12 && imageBytes[8] == 'W') {
            return "image/webp";
        }

        String normalizedUrl = imageUrl.toLowerCase();
        if (normalizedUrl.contains(".png")) {
            return "image/png";
        }
        if (normalizedUrl.contains(".webp")) {
            return "image/webp";
        }
        if (normalizedUrl.contains(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
