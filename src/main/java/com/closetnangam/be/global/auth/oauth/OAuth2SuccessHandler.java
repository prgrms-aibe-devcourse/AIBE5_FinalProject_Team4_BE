package com.closetnangam.be.global.auth.oauth;

import com.closetnangam.be.global.auth.jwt.JwtTokenProvider;
import com.closetnangam.be.global.auth.jwt.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Value("${app.oauth2.redirect-uri:http://localhost:3000}")
    private String redirectUri;

    /**
     * OAuth2 로그인 성공 시 JWT를 HttpOnly 쿠키에 담아 프론트엔드로 리다이렉트합니다.
     *
     * <p>기존 {@code ?token=} 쿼리 파라미터 방식은 브라우저 히스토리·서버 로그·Referer 헤더에
     * 토큰이 평문으로 남으므로 HttpOnly 쿠키로 전환합니다.</p>
     *
     * <p>Spring Security가 OAuth 콜백 이전 요청을 SavedRequest로 세션에 저장해 두면,
     * 리다이렉트 URL에 이전 요청의 쿼리 파라미터(예: ?token=, ?state=)가 묻어올 수 있습니다.
     * sendRedirect 전에 requestCache.removeRequest()로 SavedRequest를 명시적으로 제거합니다.</p>
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        logger.info("[OAuth2] 콜백 진입");
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = principal.getUserId();

        if (principal.isWithdrawnRestoreRequired()) {
            request.getSession(true)
                    .setAttribute(OAuth2SessionAttributes.WITHDRAWN_RESTORE_USER_ID, userId);
            expireAuthCookies(request, response);
            requestCache.removeRequest(request, response);
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, appendQuery(redirectUri, "withdrawn=restore_required"));
            return;
        }

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenService.save(userId, refreshToken);

        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(request.isSecure())   // HTTPS 환경에서는 Secure 플래그 자동 활성화
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")  // refresh/logout 경로에서만 전송
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // SavedRequest 제거 — 이전 요청의 쿼리 파라미터가 리다이렉트 URL에 묻어오는 현상 방지
        requestCache.removeRequest(request, response);
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void expireAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String appendQuery(String uri, String query) {
        return uri + (uri.contains("?") ? "&" : "?") + query;
    }
}
