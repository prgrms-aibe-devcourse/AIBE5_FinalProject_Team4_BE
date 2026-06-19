package com.closetnangam.be.global.config;

import com.closetnangam.be.global.auth.jwt.CookieBearerTokenResolver;
import com.closetnangam.be.global.auth.oauth.OAuth2FailureHandler;
import com.closetnangam.be.global.auth.oauth.OAuth2SuccessHandler;
import com.closetnangam.be.global.auth.oauth.OAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/login/oauth2/**",
            "/oauth2/**",
            "/api/v1/categories/**",
            "/api/categories/**",
            "/api/v1/clothes/registration-methods",
            "/api/naver/**",
            "/api/weather/**",
            "/api/v1/auth/**",
            "/actuator/health",
            "/actuator/health/**",
            "/api/v1/legal/**"
    };

    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * local 프로파일: API 요청은 Resource Server(JWT 쿠키)로, 소셜 로그인 흐름은 oauth2Login으로 처리합니다.
     * STATELESS를 강제하지 않아 OAuth2 로그인 세션이 유지되고, 성공 후 JWT 쿠키로 전환됩니다.
     */
    @Bean
    @Profile("local")
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(e -> e.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }

    /**
     * 비-local 프로파일 탈퇴 계정 복구 체인 (@Order 0): OAuth 세션의 복구 대기 정보를 읽습니다.
     */
    @Bean
    @Profile("!local")
    @Order(0)
    public SecurityFilterChain authRestoreSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/auth/restore-withdrawn")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 비-local 프로파일 API 체인 (@Order 1): /api/** 요청을 Resource Server(JWT)로 인증합니다.
     * STATELESS로 운영되며 세션을 생성하지 않습니다.
     */
    @Bean
    @Profile("!local")
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(SecurityConfig::isApiRequest)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }

    /**
     * 비-local 프로파일 OAuth 체인 (@Order 2): 소셜 로그인 시작/콜백 경로만 처리합니다.
     * 일반 FE 경로(/, /login 등)는 Spring Security 기본 로그인 페이지로 개입하지 않도록 제외합니다.
     */
    @Bean
    @Profile("!local")
    @Order(2)
    public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(SecurityConfig::isOAuthRequest)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(e -> e.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );

        return http.build();
    }

    static boolean isApiRequest(HttpServletRequest request) {
        return hasPathPrefix(request, "/api");
    }

    static boolean isOAuthRequest(HttpServletRequest request) {
        return hasPathPrefix(request, "/oauth2") || hasPathPrefix(request, "/login/oauth2");
    }

    private static boolean hasPathPrefix(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        return uri != null && (uri.equals(prefix) || uri.startsWith(prefix + "/"));
    }
}
