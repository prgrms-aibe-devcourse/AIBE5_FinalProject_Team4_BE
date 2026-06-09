package com.closetnangam.be.global.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    /** 48바이트 secret — Keys.hmacShaKeyFor만 쓰면 HS384가 선택될 수 있는 길이 */
    private static final String LONG_SECRET = "a".repeat(48);

    @Test
    void createAndValidateRoundTripWithProvider() {
        JwtTokenProvider provider = new JwtTokenProvider(LONG_SECRET, 3_600_000L, 7_000_000L);
        String token = provider.createAccessToken(1L);
        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    void providerTokenIsAcceptedByResourceServerDecoder() {
        JwtTokenProvider provider = new JwtTokenProvider(LONG_SECRET, 3_600_000L,  7_000_000L);
        String token = provider.createAccessToken(42L);

        SecretKey secretKey = JwtSecretKeys.hs256SecretKey(LONG_SECRET);
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
    }
}
