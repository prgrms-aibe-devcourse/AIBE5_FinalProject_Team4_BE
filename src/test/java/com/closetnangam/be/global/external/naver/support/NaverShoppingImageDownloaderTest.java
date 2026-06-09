package com.closetnangam.be.global.external.naver.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverShoppingImageDownloaderTest {

    @Test
    @DisplayName("PNG 시그니처를 이미지로 인식한다")
    void detectsPngSignature() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(NaverShoppingImageDownloader.looksLikeImage(png)).isTrue();
    }

    @Test
    @DisplayName("JPEG 시그니처를 이미지로 인식한다")
    void detectsJpegSignature() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertThat(NaverShoppingImageDownloader.looksLikeImage(jpeg)).isTrue();
    }

    @Test
    @DisplayName("HTML 에러 본문은 이미지가 아니다")
    void rejectsHtmlBody() {
        byte[] html = "<html>not found</html>".getBytes();
        assertThat(NaverShoppingImageDownloader.looksLikeImage(html)).isFalse();
    }
}
