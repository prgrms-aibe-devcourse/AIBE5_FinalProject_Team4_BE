package com.closetnangam.be.domain.outfit.repository;

import com.closetnangam.be.domain.outfit.entity.OutfitBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OutfitBookRepository extends JpaRepository<OutfitBook, Long> {

    Optional<OutfitBook> findByUser_Id(Long userId);

    @Query("""
            select ob
            from OutfitBook ob
            where ob.id = :bookId
              and ob.user.id = :userId
            """)
    Optional<OutfitBook> findByIdAndUserId(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId
    );
}
