package com.backoffice.pos.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByBusinessIdOrderByNameAsc(Long businessId);

    Page<Role> findByBusinessId(Long businessId, Pageable pageable);

    Page<Role> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    Optional<Role> findByIdAndBusinessId(Long id, Long businessId);

    Optional<Role> findByBusinessIdAndName(Long businessId, String name);

    boolean existsByBusinessIdAndName(Long businessId, String name);
}
