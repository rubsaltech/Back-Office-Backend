package com.backoffice.pos.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findByBusinessId(Long businessId, Pageable pageable);

    Page<Category> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    List<Category> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Category> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndName(Long businessId, String name);

    long countByBusinessId(Long businessId);
}
