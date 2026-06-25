package com.closetnangam.be.domain.outfit.repository;

import com.closetnangam.be.domain.outfit.entity.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    @Query("""
            select o
            from Outfit o
            join fetch o.outfitBook ob
            where ob.id = :bookId
              and o.deletedAt is null
            order by o.createdAt desc
            """)
    List<Outfit> findAllByOutfitBookId(@Param("bookId") Long bookId);

    @Query("""
            select o
            from Outfit o
            where o.outfitId = :outfitId
              and o.outfitBook.id = :bookId
              and o.deletedAt is null
            """)
    Optional<Outfit> findActiveByOutfitIdAndOutfitBook_Id(@Param("outfitId") Long outfitId, @Param("bookId") Long bookId);

    @Query("""
            select o
            from Outfit o
            join fetch o.outfitBook ob
            where o.outfitId = :outfitId
              and ob.user.id = :userId
              and o.deletedAt is null
            """)
    Optional<Outfit> findActiveByOutfitIdAndUserId(@Param("outfitId") Long outfitId, @Param("userId") Long userId);

    @Query("""
            select o
            from Outfit o
            join fetch o.outfitBook ob
            join fetch ob.user
            where o.outfitId = :outfitId
              and o.deletedAt is null
            """)
    Optional<Outfit> findActiveByOutfitId(@Param("outfitId") Long outfitId);

    @Query("""
            select o
            from Outfit o
            join o.outfitBook ob
            where ob.user.id = :userId
              and o.deletedAt is null
              and o.description like concat(:sourceMarker, '%')
            """)
    Optional<Outfit> findActiveFeedSaveClone(
            @Param("userId") Long userId,
            @Param("sourceMarker") String sourceMarker
    );
}
