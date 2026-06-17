package com.closetnangam.be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 옷·피드 이미지는 /api/v1/images/** 로 인증 사용자에게 서빙합니다.
// 구매내역 캡처만 업로드 소유자 본인 조회, 옷/피드는 로그인 사용자 공유 조회입니다.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
