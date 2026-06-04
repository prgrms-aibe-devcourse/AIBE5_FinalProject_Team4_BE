package com.closetnangam.be.global.auth.jwt;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "jwt.secret은 32바이트(256bit) 이상이어야 합니다. 현재 " + keyBytes.length + "바이트."
            );
        }
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
