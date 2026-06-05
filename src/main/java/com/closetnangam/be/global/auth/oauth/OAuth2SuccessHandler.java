package com.closetnangam.be.global.auth.oauth;

import com.closetnangam.be.global.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000}")
    private String redirectUri;

    /**
     * OAuth2 로그인 성공 시 JWT를 HttpOnly 쿠키에 담아 프론트엔드로 리다이렉트합니다.
     *
     * <p>기존 {@code ?token=} 쿼리 파라미터 방식은 브라우저 히스토리·서버 로그·Referer 헤더에
     * 토큰이 평문으로 남으므로 HttpOnly 쿠키로 전환합니다.</p>
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        String token = jwtTokenProvider.createAccessToken(principal.getUserId());

        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(request.isSecure())   // HTTPS 환경에서는 Secure 플래그 자동 활성화
                .sameSite("Lax")
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // FE 마이그레이션: localStorage + ?token= 연동이 쿠키 전환 전까지 동작하도록 쿼리도 함께 전달
        String targetUrl = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "token=" + token;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
