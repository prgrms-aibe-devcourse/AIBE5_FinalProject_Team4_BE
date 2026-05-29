package com.closetnangam.be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 이미지 파일은 /api/v1/images/** 엔드포인트(ImageController)를 통해 인증 후 서빙합니다.
// Spring Security anyRequest().authenticated() 보호가 적용되므로 별도 static resource handler를 두지 않습니다.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
