package com.closetnangam.be.domain.outfit.repository;

import com.closetnangam.be.domain.outfit.entity.OutfitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {

    /**
     * 코디북 재조회 응답에서 저장된 코디 구성을 복원하기 위한 조회.
     *
     * ClothesResponse가 스타일 태그를 함께 사용하므로 styleTags/Style을 미리 로딩한다.
     * colorTags는 Clothes 엔티티의 SUBSELECT 전략을 따라 별도 쿼리로 묶어서 로딩된다.
     */
    @Query("""
            select distinct oi
            from OutfitItem oi
            join fetch oi.outfit o
            join fetch oi.clothes c
            left join fetch c.styleTags st
            left join fetch st.style
            where o.outfitBook.id = :bookId
            order by o.createdAt desc, oi.layerOrder asc, oi.id asc
            """)
    List<OutfitItem> findAllByOutfitBookId(@Param("bookId") Long bookId);
}
