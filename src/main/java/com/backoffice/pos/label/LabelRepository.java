package com.backoffice.pos.label;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {

    Page<Label> findByBusinessId(Long businessId, Pageable pageable);

    Page<Label> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    List<Label> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Label> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndName(Long businessId, String name);
}
