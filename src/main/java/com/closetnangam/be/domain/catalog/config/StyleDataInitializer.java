package com.closetnangam.be.domain.catalog.config;

import com.closetnangam.be.domain.catalog.entity.Style;
import com.closetnangam.be.domain.catalog.enums.StyleCode;
import com.closetnangam.be.domain.catalog.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StyleDataInitializer implements ApplicationRunner {

    private final StyleRepository styleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (StyleCode styleCode : StyleCode.values()) {
            if (styleRepository.existsByCode(styleCode.name())) {
                continue;
            }
            styleRepository.save(Style.from(styleCode));
        }
    }
}
