package com.closetnangam.be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 인증 없이 접근 허용할 URL 목록
    // API 경로는 /api/v1 prefix 기준으로 통일합니다.
    // 레거시 /api/categories/** 는 FE 마이그레이션 기간 동안만 허용하며, 이후 제거 예정입니다.
    private static final String[] PUBLIC_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/login/**",
            "/oauth2/**",
            "/api/v1/categories/**",
            "/api/categories/**",
            "/api/v1/clothes/registration-methods"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()  // 위 URL은 누구나 접근 가능
                        .anyRequest().authenticated()              // 나머지는 인증 필요
                )
                .oauth2Login(Customizer.withDefaults());  // 기본 OAuth2 로그인 설정 사용

        return http.build();
    }
}
