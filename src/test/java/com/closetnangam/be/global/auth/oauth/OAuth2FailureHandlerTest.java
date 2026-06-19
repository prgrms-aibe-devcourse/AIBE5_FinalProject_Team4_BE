package com.closetnangam.be.global.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuth2FailureHandlerTest {

    @Test
    void redirectsToFrontendWithOauthFailureQuery() throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "redirectUri", "https://front.example.com");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/kakao");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("token exchange failed"));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://front.example.com?error=oauth_failed");
    }

    @Test
    void preservesExistingFrontendQuery() {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();

        assertThat(handler.appendQuery("https://front.example.com/auth?from=oauth", "error=oauth_failed"))
                .isEqualTo("https://front.example.com/auth?from=oauth&error=oauth_failed");
    }
}
