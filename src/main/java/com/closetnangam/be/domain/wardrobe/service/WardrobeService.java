package com.closetnangam.be.domain.wardrobe.service;

import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeResponse;
import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import com.closetnangam.be.domain.wardrobe.repository.WardrobeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardrobeService {

    private final WardrobeRepository wardrobeRepository;
    private final UserRepository userRepository;

    @Transactional
    public WardrobeResponse getWardrobeByUserId(Long userId) {
        return WardrobeResponse.from(getOrCreateWardrobe(userId));
    }

    @Transactional
    public WardrobeResponse createWardrobe(Long userId) {
        if (wardrobeRepository.existsByUser_Id(userId)) {
            throw new IllegalStateException("이미 옷장이 존재합니다.");
        }
        return WardrobeResponse.from(createWardrobeEntity(userId));
    }

    @Transactional
    public Wardrobe getOrCreateWardrobe(Long userId) {
        // User row에 FOR UPDATE lock: User는 항상 존재하므로 신규 사용자에게도 lock이 보장됨.
        // 같은 userId로 동시 요청이 들어올 때 두 트랜잭션이 직렬화되어
        // 중복 INSERT → DataIntegrityViolationException을 방지함.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return wardrobeRepository.findByUser_Id(userId)
                .orElseGet(() -> wardrobeRepository.save(Wardrobe.create(user)));
    }

    private Wardrobe createWardrobeEntity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return wardrobeRepository.save(Wardrobe.create(user));
    }
}
