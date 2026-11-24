package com.backoffice.pos.servicejob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceJobUpdateRepository extends JpaRepository<ServiceJobUpdate, Long> {

    List<ServiceJobUpdate> findByServiceJobIdOrderByCreatedAtAsc(Long serviceJobId);
}
