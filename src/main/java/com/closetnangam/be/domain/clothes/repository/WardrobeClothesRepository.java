package com.closetnangam.be.domain.clothes.repository;

import com.closetnangam.be.domain.clothes.entity.WardrobeClothes;
import com.closetnangam.be.domain.clothes.enums.OwnershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WardrobeClothesRepository extends JpaRepository<WardrobeClothes, Long> {

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.wardrobe w
            join fetch w.user
            join fetch wc.clothes c
            where wc.id = :wardrobeClothesId
            """)
    Optional<WardrobeClothes> findByIdWithDetails(@Param("wardrobeClothesId") Long wardrobeClothesId);

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
            order by c.createdAt desc
            """)
    List<WardrobeClothes> findAllByUserIdAndOwnershipStatus(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select distinct wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where w.user.id = :userId
              and wc.ownershipStatus = :ownershipStatus
              and wc.favorite = true
            order by wc.updatedAt desc
            """)
    List<WardrobeClothes> findFavoritesByUserIdAndOwnershipStatus(
            @Param("userId") Long userId,
            @Param("ownershipStatus") OwnershipStatus ownershipStatus
    );

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            join fetch w.user
            where c.id = :clothesId
            """)
    Optional<WardrobeClothes> findByClothesIdWithDetails(@Param("clothesId") Long clothesId);

    @Query("""
            select wc from WardrobeClothes wc
            join fetch wc.clothes c
            join fetch wc.wardrobe w
            where c.id = :clothesId and w.user.id = :userId
            """)
    Optional<WardrobeClothes> findByClothesIdAndUserId(
            @Param("clothesId") Long clothesId,
            @Param("userId") Long userId
    );

    void deleteByClothes_Id(Long clothesId);
}
