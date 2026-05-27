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

    public WardrobeResponse getWardrobeByUserId(Long userId) {
        Wardrobe wardrobe = wardrobeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("옷장을 찾을 수 없습니다."));
        return WardrobeResponse.from(wardrobe);
    }

    @Transactional
    public WardrobeResponse createWardrobe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (wardrobeRepository.existsByUser_Id(userId)) {
            throw new IllegalStateException("이미 옷장이 존재합니다.");
        }

        Wardrobe wardrobe = wardrobeRepository.save(Wardrobe.create(user));
        return WardrobeResponse.from(wardrobe);
    }

    @Transactional
    public WardrobeResponse getOrCreateWardrobe(Long userId) {
        return wardrobeRepository.findByUser_Id(userId)
                .map(WardrobeResponse::from)
                .orElseGet(() -> createWardrobe(userId));
    }
}
