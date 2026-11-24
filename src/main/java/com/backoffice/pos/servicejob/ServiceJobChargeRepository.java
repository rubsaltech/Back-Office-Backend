package com.backoffice.pos.servicejob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceJobChargeRepository extends JpaRepository<ServiceJobCharge, Long> {

    List<ServiceJobCharge> findByServiceJobIdOrderByCreatedAtAsc(Long serviceJobId);

    Optional<ServiceJobCharge> findByIdAndServiceJobId(Long id, Long serviceJobId);
}
