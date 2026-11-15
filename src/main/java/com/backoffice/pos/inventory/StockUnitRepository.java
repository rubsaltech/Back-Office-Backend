package com.backoffice.pos.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockUnitRepository extends JpaRepository<StockUnit, Long> {

    List<StockUnit> findByVariantIdOrderByCreatedAtAsc(Long variantId);

    List<StockUnit> findByVariantIdAndStatus(Long variantId, UnitStatus status);

    List<StockUnit> findByOrderItemIdAndStatus(Long orderItemId, UnitStatus status);

    long countByVariantIdAndStatus(Long variantId, UnitStatus status);

    boolean existsByBusinessIdAndSerial(Long businessId, String serial);

    Optional<StockUnit> findByBusinessIdAndSerial(Long businessId, String serial);
}
