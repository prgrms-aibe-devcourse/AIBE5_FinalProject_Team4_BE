package com.closetnangam.be.domain.ai.repository;

import com.closetnangam.be.domain.ai.entity.ClothingAiPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClothingAiPhotoRepository extends JpaRepository<ClothingAiPhoto, Long> {

    Optional<ClothingAiPhoto> findByIdAndUser_Id(Long id, Long userId);
}
