package com.closetnangam.be.global.auth.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

@Configuration
public class JwtAuthenticationConverterConfig {

    /**
     * Resource Server JWT의 principal을 Long(userId)로 맞춰 SecurityUtils와 호환합니다.
     *
     * <p>delegate.convert()가 null을 반환하더라도 NPE 없이 빈 권한 목록으로 폴백합니다.</p>
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        return jwt -> {
            String tokenType = jwt.getClaimAsString("type");
            if (!"access".equals(tokenType)) {
                throw new org.springframework.security.oauth2.server.resource.InvalidBearerTokenException(
                  "Invalid token type: " + tokenType
                );
            }
            AbstractAuthenticationToken converted = delegate.convert(jwt);
            Long userId = Long.valueOf(jwt.getSubject());
            return new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    converted != null ? converted.getAuthorities() : List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        };
    }
}
