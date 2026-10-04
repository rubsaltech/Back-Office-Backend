package com.backoffice.pos.floor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableRepository extends JpaRepository<RestaurantTable, Long> {

    Page<RestaurantTable> findByBusinessId(Long businessId, Pageable pageable);

    Page<RestaurantTable> findByBusinessIdAndFloor_Id(Long businessId, Long floorId, Pageable pageable);

    Page<RestaurantTable> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    Optional<RestaurantTable> findByIdAndBusinessId(Long id, Long businessId);

    long countByFloor_Id(Long floorId);

    // --- store-scoped ---
    Page<RestaurantTable> findByStoreId(Long storeId, Pageable pageable);

    Page<RestaurantTable> findByStoreIdAndFloor_Id(Long storeId, Long floorId, Pageable pageable);

    Page<RestaurantTable> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    Optional<RestaurantTable> findByIdAndStoreId(Long id, Long storeId);
}
