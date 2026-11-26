package com.backoffice.pos.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Page<Customer> findByStoreIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long storeId, String name, Pageable pageable);

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);

    long countByStoreId(Long storeId);
}
