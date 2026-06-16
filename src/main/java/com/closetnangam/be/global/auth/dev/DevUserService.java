package com.closetnangam.be.global.auth.dev;

import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로필 전용: mock-token JWT의 userId에 대응하는 users row를 보장합니다.
 * mock-token은 토큰만 발급하므로, DB에 사용자가 없으면 사진 업로드 등이 실패합니다.
 */
@Service
@Profile("local")
@RequiredArgsConstructor
public class DevUserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final OutfitBookRepository outfitBookRepository;

    @Transactional
    public User ensureDevUser(Long userId) {
        User user = userRepository.findById(userId).orElseGet(() -> {
            entityManager.createNativeQuery(
                            """
                                    INSERT INTO users (
                                        user_id, nickname, email, profile_image_url, profile_bio,
                                        external_link_url, birth_date, gender, region_name, region_code,
                                        marketing_agreed, marketing_agreed_at, status, withdrawn_at,
                                        created_at, updated_at
                                    )
                                    VALUES (
                                        :id, :nickname, :email, '', '', '',
                                        '2000-01-01', 'OTHER', '', '', 0, '1970-01-01 00:00:00',
                                        'ACTIVE', NULL, NOW(), NOW()
                                    )
                                    """
                    )
                    .setParameter("id", userId)
                    .setParameter("nickname", "dev-user-" + userId)
                    .setParameter("email", "dev" + userId + "@local.test")
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("개발용 사용자 생성에 실패했습니다. userId=" + userId));
        });

        if (outfitBookRepository.findByUser_Id(user.getId()).isEmpty()) {
            outfitBookRepository.save(OutfitBook.create(user));
        }

        return user;
    }
}
