package com.closetnangam.be.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigTest {

    @Test
    void apiMatcherOnlyMatchesApiPathPrefix() {
        assertThat(SecurityConfig.isApiRequest(request("/api/v1/users/profile"))).isTrue();
        assertThat(SecurityConfig.isApiRequest(request("/api"))).isTrue();
        assertThat(SecurityConfig.isApiRequest(request("/oauth2/authorization/kakao"))).isFalse();
        assertThat(SecurityConfig.isApiRequest(request("/apiv1/users/profile"))).isFalse();
    }

    @Test
    void oauthMatcherOnlyMatchesOauthStartAndCallbackPaths() {
        assertThat(SecurityConfig.isOAuthRequest(request("/oauth2/authorization/kakao"))).isTrue();
        assertThat(SecurityConfig.isOAuthRequest(request("/login/oauth2/code/kakao"))).isTrue();

        assertThat(SecurityConfig.isOAuthRequest(request("/login"))).isFalse();
        assertThat(SecurityConfig.isOAuthRequest(request("/login/"))).isFalse();
        assertThat(SecurityConfig.isOAuthRequest(request("/"))).isFalse();
        assertThat(SecurityConfig.isOAuthRequest(request("/api/v1/users/profile"))).isFalse();
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
