package com.closetnangam.be.domain.catalog.repository;

import com.closetnangam.be.domain.catalog.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StyleRepository extends JpaRepository<Style, Long> {

    Optional<Style> findByCode(String code);

    List<Style> findByCodeIn(List<String> codes);

    List<Style> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);
}
