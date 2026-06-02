package com.closetnangam.be.global.common.validation;

public final class ValidationPatterns {

    /**
     * SSRF 방어를 위해 내부 네트워크 주소(localhost, 127.x, 10.x, 192.168.x, 172.16~31.x, 169.254.x)를
     * 차단하고 https 스킴만 허용합니다.
     */
    public static final String HTTP_URL =
            "^https://(?!localhost|127\\.0\\.0\\.1|10\\.|192\\.168\\.|172\\.1[6-9]\\.|172\\.2[0-9]\\.|172\\.3[01]\\.|169\\.254\\.).+";

    private ValidationPatterns() {
    }
}
