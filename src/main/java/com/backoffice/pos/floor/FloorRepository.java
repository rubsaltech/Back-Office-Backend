package com.backoffice.pos.floor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Floor> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndName(Long businessId, String name);

    // --- store-scoped ---
    List<Floor> findByStoreIdOrderByNameAsc(Long storeId);

    Optional<Floor> findByIdAndStoreId(Long id, Long storeId);

    boolean existsByStoreIdAndName(Long storeId, String name);
}
