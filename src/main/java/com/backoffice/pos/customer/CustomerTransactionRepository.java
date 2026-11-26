package com.backoffice.pos.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
