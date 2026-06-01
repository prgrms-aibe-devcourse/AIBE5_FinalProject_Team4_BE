package com.closetnangam.be.domain.outfit.service;

import com.closetnangam.be.domain.outfit.dto.request.OutfitCreateRequest;
import com.closetnangam.be.domain.outfit.dto.response.OutfitBookResponse;
import com.closetnangam.be.domain.outfit.dto.response.OutfitResponse;
import com.closetnangam.be.domain.outfit.entity.Outfit;
import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import com.closetnangam.be.domain.outfit.repository.OutfitBookRepository;
import com.closetnangam.be.domain.outfit.repository.OutfitRepository;
import com.closetnangam.be.domain.user.entity.User;
import com.closetnangam.be.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutfitService {

    private final OutfitBookRepository outfitBookRepository;
    private final OutfitRepository outfitRepository;
    private final UserRepository userRepository;

    @Transactional
    public OutfitBookResponse createBook(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (outfitBookRepository.findByUser_Id(userId).isPresent()) {
            throw new IllegalStateException("이미 코디북이 존재합니다.");
        }

        OutfitBook outfitBook = outfitBookRepository.save(OutfitBook.create(user));
        return OutfitBookResponse.from(outfitBook, userId, List.of());
    }

    @Transactional
    public OutfitResponse createOutfit(Long bookId, Long userId, OutfitCreateRequest request) {
        OutfitBook outfitBook = outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));

        Outfit outfit = outfitRepository.save(request.toEntity(outfitBook));
        return OutfitResponse.from(outfit);
    }

    public OutfitBookResponse getBookByUserId(Long userId) {
        OutfitBook outfitBook = outfitBookRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));
        return toBookResponse(outfitBook, userId);
    }

    public OutfitBookResponse getBookById(Long bookId, Long userId) {
        OutfitBook outfitBook = outfitBookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new EntityNotFoundException("코디북을 찾을 수 없습니다."));
        return toBookResponse(outfitBook, userId);
    }

    private OutfitBookResponse toBookResponse(OutfitBook outfitBook, Long userId) {
        List<Outfit> outfits = outfitRepository.findAllByOutfitBookId(outfitBook.getId());
        return OutfitBookResponse.from(outfitBook, userId, outfits);
    }
}
