package com.closetnangam.be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 출처 — 쿠키 전송 시 와일드카드(*) 사용 불가, 정확한 출처 명시 필요
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",  // FE 개발 서버
                "http://localhost:3000"   // 대체 FE 포트
        ));

        // 허용할 HTTP 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 요청 헤더
        config.setAllowedHeaders(List.of("*"));

        // credentials: 'include' 요청(쿠키 포함)을 허용
        config.setAllowCredentials(true);

        // preflight 캐시 시간 (초)
        config.setMaxAge(3600L);
        // static 이미지용 CORS (credentials 불필요)
        CorsConfiguration imageConfig = new CorsConfiguration();
        imageConfig.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        imageConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        imageConfig.setAllowedHeaders(List.of("*"));
        imageConfig.setAllowCredentials(false);
        imageConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<Filter> imagesCorsFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((request, response, chain) -> {
            HttpServletResponse res = (HttpServletResponse) response;
            HttpServletRequest req = (HttpServletRequest) request;
            String origin = req.getHeader("Origin");
            if (origin != null && (origin.equals("http://localhost:5173") || origin.equals("http://localhost:3000"))) {
                res.setHeader("Access-Control-Allow-Origin", origin);
                res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
                res.setHeader("Access-Control-Max-Age", "3600");
            }
            chain.doFilter(request, response);
        });
        bean.addUrlPatterns("/images/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
