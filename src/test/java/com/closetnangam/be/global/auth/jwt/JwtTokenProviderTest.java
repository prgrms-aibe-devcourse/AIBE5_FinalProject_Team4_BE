package com.closetnangam.be.global.auth.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void createAndValidateRoundTrip() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "sasefsdfklzxclkmcflksfjelwsifjkfsdfjkewr",
                3600000L
        );
        String token = provider.createAccessToken(1L);
        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(1L);
    }
}
