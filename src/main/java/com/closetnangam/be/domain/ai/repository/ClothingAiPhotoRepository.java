package com.closetnangam.be.domain.ai.repository;

import com.closetnangam.be.domain.ai.entity.ClothingAiPhoto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClothingAiPhotoRepository extends JpaRepository<ClothingAiPhoto, Long> {

    Optional<ClothingAiPhoto> findByIdAndUser_Id(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ClothingAiPhoto p WHERE p.id = :id AND p.user.id = :userId")
    Optional<ClothingAiPhoto> findByIdAndUser_IdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
