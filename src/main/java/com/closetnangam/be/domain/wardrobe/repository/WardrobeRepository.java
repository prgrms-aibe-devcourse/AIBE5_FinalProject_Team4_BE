package com.closetnangam.be.domain.wardrobe.repository;

import com.closetnangam.be.domain.wardrobe.entity.Wardrobe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WardrobeRepository extends JpaRepository<Wardrobe, Long> {

    Optional<Wardrobe> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
