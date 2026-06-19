package com.closetnangam.be.global.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Value("${app.oauth2.redirect-uri:http://localhost:3000}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.warn("[OAuth2] 로그인 실패. uri={}, reason={}", request.getRequestURI(), exception.getMessage());
        requestCache.removeRequest(request, response);
        clearAuthenticationAttributes(request);
        redirectStrategy.sendRedirect(request, response, appendQuery(redirectUri, "error=oauth_failed"));
    }

    String appendQuery(String uri, String query) {
        return uri + (uri.contains("?") ? "&" : "?") + query;
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}
