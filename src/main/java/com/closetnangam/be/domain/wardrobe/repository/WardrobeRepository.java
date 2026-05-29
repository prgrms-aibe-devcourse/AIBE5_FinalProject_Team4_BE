package com.closetnangam.be.domain.wardrobe.repository;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WardrobeRepository extends JpaRepository<Wardrobe, Long> {

    Optional<Wardrobe> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wardrobe w WHERE w.user.id = :userId")
    Optional<Wardrobe> findByUser_IdForUpdate(@Param("userId") Long userId);
}
