package com.backoffice.pos.servicejob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ServiceJobRepository extends JpaRepository<ServiceJob, Long> {

    boolean existsByBusinessIdAndToken(Long businessId, String token);

    Optional<ServiceJob> findByBusinessIdAndTokenIgnoreCase(Long businessId, String token);

    Optional<ServiceJob> findByIdAndStoreId(Long id, Long storeId);

    Page<ServiceJob> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Page<ServiceJob> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, ServiceJobStatus status, Pageable pageable);

    Page<ServiceJob> findByStoreIdAndStatusInOrderByCreatedAtDesc(Long storeId, Collection<ServiceJobStatus> statuses, Pageable pageable);
}
