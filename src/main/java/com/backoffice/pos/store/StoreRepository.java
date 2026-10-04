package com.backoffice.pos.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByBusinessIdOrderByCreatedAtAsc(Long businessId);

    Optional<Store> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByIdAndBusinessId(Long id, Long businessId);

    Optional<Store> findFirstByBusinessIdAndMainTrue(Long businessId);

    Optional<Store> findFirstByBusinessIdOrderByCreatedAtAsc(Long businessId);
}
