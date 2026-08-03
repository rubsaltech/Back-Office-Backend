package com.backoffice.pos.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Page<Employee> findByBusinessId(Long businessId, Pageable pageable);

    Page<Employee> findByBusinessIdAndFullNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    Optional<Employee> findByIdAndBusinessId(Long id, Long businessId);

    Optional<Employee> findByBusinessIdAndEmailIgnoreCase(Long businessId, String email);

    boolean existsByBusinessIdAndEmailIgnoreCase(Long businessId, String email);

    List<Employee> findByStore_Id(Long storeId);

    long countByBusinessId(Long businessId);

    List<Employee> findTop6ByBusinessIdOrderBySalesTotalDesc(Long businessId);
}
