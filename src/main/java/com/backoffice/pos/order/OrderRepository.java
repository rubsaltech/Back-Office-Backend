package com.backoffice.pos.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByBusinessId(Long businessId, Pageable pageable);

    Page<Order> findByBusinessIdAndStatus(Long businessId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessId(Long businessId);

    /** Highest order number issued for a business (for the next running number). */
    Optional<Order> findTopByBusinessIdOrderByOrderNumberDesc(Long businessId);
}
