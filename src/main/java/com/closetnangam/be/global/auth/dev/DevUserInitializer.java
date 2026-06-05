package com.closetnangam.be.global.auth.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local 프로필 기동 시 mock-token 기본 사용자(userId=1)를 DB에 준비합니다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevUserInitializer implements ApplicationRunner {

    public static final long DEFAULT_DEV_USER_ID = 1L;

    private final DevUserService devUserService;

    @Override
    public void run(ApplicationArguments args) {
        devUserService.ensureDevUser(DEFAULT_DEV_USER_ID);
    }
}
