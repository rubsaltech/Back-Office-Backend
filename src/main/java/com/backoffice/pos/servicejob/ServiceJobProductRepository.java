package com.backoffice.pos.servicejob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceJobProductRepository extends JpaRepository<ServiceJobProduct, Long> {

    List<ServiceJobProduct> findByServiceJobIdOrderByCreatedAtAsc(Long serviceJobId);

    Optional<ServiceJobProduct> findByIdAndServiceJobId(Long id, Long serviceJobId);
}
