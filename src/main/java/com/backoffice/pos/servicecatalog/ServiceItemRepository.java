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

    // --- store-scoped (catalog is per store) ---
    Page<ServiceItem> findByStoreId(Long storeId, Pageable pageable);

    Page<ServiceItem> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    Optional<ServiceItem> findByIdAndStoreId(Long id, Long storeId);
}
