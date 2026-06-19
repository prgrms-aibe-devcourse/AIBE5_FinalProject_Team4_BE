package com.closetnangam.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final List<String> LOCAL_DEV_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000"
    );

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        List<String> origins = resolveAllowedOrigins();
        if (origins.isEmpty()) {
            return source;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Profile("local")
    public FilterRegistrationBean<Filter> imagesCorsFilter() {
        List<String> origins = resolveAllowedOrigins();
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((request, response, chain) -> {
            HttpServletResponse res = (HttpServletResponse) response;
            HttpServletRequest req = (HttpServletRequest) request;
            String origin = req.getHeader("Origin");
            if (origin != null && origins.contains(origin)) {
                res.setHeader("Access-Control-Allow-Origin", origin);
                res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
                res.setHeader("Access-Control-Max-Age", "3600");
            }
            chain.doFilter(request, response);
        });
        bean.addUrlPatterns("/api/v1/images/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    private List<String> resolveAllowedOrigins() {
        if (StringUtils.hasText(allowedOrigins)) {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return LOCAL_DEV_ORIGINS;
    }
}