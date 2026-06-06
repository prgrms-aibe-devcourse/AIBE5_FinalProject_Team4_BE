package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.Clothes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {

    Optional<Clothes> findByExternalProductId(String externalProductId);

    @Query("SELECT c FROM Clothes c ORDER BY c.createdAt DESC")
    List<Clothes> findAllForRecommendation(Pageable pageable);
}
