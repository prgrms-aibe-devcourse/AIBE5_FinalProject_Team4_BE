package com.closetnangam.be.domain.outfit.config;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@Order(2) // RecommendationSampleDataInitializer (default order) 이후에 실행되도록 설정
public class OutfitSampleDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final OutfitBookRepository outfitBookRepository;
    private final OutfitRepository outfitRepository;

    private static final String ANCHOR_EMAIL = "demo.recommend.anchor@example.com";
    private static final String CATALOG_EMAIL = "demo.recommend.catalog@example.com";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting Outfit sample data initialization...");

        initializeForUser(ANCHOR_EMAIL, "멋진 남성 데일리룩", "심플한 블랙 셔츠와 데님을 매치한 깔끔한 룩입니다.");
        initializeForUser(CATALOG_EMAIL, "봄맞이 화사한 데이트룩", "화이트 슬랙스와 베이지 니트로 연출한 부드러운 이미지의 룩입니다.");

        log.info("Outfit sample data initialization completed.");
    }

    private void initializeForUser(String email, String outfitTitle, String outfitDesc) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("User with email {} not found. Skipping outfit data for this user.", email);
            return;
        }

        User user = userOpt.get();
        OutfitBook outfitBook = outfitBookRepository.findByUser_Id(user.getId())
                .orElseGet(() -> outfitBookRepository.save(OutfitBook.create(user)));

        if (outfitRepository.findAllByOutfitBookId(outfitBook.getId()).isEmpty()) {
            createSampleOutfits(outfitBook, outfitTitle, outfitDesc);
        }
    }

    private void createSampleOutfits(OutfitBook outfitBook, String title, String description) {
        outfitRepository.save(Outfit.builder()
                .outfitBook(outfitBook)
                .title(title)
                .description(description)
                .thumbnailUrl("https://images.unsplash.com/photo-1512436991641-6745cdb1723f?q=80&w=500")
                .situation("DAILY")
                .season("SPRING")
                .favorite(true)
                .build());

        outfitRepository.save(Outfit.builder()
                .outfitBook(outfitBook)
                .title(title + " (버전 2)")
                .description(description + " 상황에 따라 액세서리를 추가하면 좋습니다.")
                .thumbnailUrl("https://images.unsplash.com/photo-1521335629791-ce4aec67dd16?q=80&w=500")
                .situation("DATE")
                .season("SPRING")
                .favorite(false)
                .build());
    }
}
