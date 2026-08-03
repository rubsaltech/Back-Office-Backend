package com.backoffice.pos.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByBusinessIdOrderByKeyAsc(Long businessId);

    Page<Permission> findByBusinessId(Long businessId, Pageable pageable);

    Page<Permission> findByBusinessIdAndKeyContainingIgnoreCase(Long businessId, String key, Pageable pageable);

    Optional<Permission> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndKey(Long businessId, String key);
}
