package com.closetnangam.be.global.auth.jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT HMAC 서명/검증용 secret key 생성.
 *
 * <p>JJWT {@code Keys.hmacShaKeyFor(byte[])}는 바이트 길이에 따라 HS256/384/512를 선택하지만,
 * Resource Server {@code NimbusJwtDecoder} 기본값은 HS256이므로 알고리즘을 HS256으로 고정합니다.</p>
 */
public final class JwtSecretKeys {

    private static final String HS256 = "HmacSHA256";
    private static final int MIN_KEY_BYTES = 32;

    private JwtSecretKeys() {
    }

    public static SecretKey hs256SecretKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "jwt.secret은 32바이트(256bit) 이상이어야 합니다. 현재 " + keyBytes.length + "바이트."
            );
        }
        return new SecretKeySpec(keyBytes, HS256);
    }
}
