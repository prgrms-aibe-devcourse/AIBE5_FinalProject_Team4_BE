package com.closetnangam.be.global.common.validation;

public final class ValidationPatterns {

    /**
     * 서버 업로드/저장용 이미지 URL.
     * 로컬 스토리지({@code http://localhost:8080/api/v1/images/...})와 https CDN URL 모두 허용합니다.
     * 이 필드는 저장용 문자열이며 서버-side fetch 대상이 아닙니다.
     */
    public static final String STORED_IMAGE_URL = "^https?://.+";

    /**
     * 외부 쇼핑 상품 URL.
     * https만 허용하고 내부 네트워크 주소(localhost, RFC1918 등)는 차단합니다.
     */
    public static final String EXTERNAL_HTTPS_URL =
            "^https://(?!localhost|127\\.0\\.0\\.1|10\\.|192\\.168\\.|172\\.1[6-9]\\.|172\\.2[0-9]\\.|172\\.3[01]\\.|169\\.254\\.).+";

    private ValidationPatterns() {
    }
}
