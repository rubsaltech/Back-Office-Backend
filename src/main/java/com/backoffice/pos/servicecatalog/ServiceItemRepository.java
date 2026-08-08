package com.backoffice.pos.servicecatalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    Page<ServiceItem> findByBusinessId(Long businessId, Pageable pageable);

    Page<ServiceItem> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    Optional<ServiceItem> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessId(Long businessId);
}
