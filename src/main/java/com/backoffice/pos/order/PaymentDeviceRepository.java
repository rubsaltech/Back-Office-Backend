package com.backoffice.pos.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentDeviceRepository extends JpaRepository<PaymentDevice, Long> {

    List<PaymentDevice> findByBusinessIdOrderByCreatedAtAsc(Long businessId);

    Optional<PaymentDevice> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndSerialNumber(Long businessId, String serialNumber);
}
