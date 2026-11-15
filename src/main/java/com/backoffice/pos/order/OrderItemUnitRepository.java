package com.backoffice.pos.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderItemUnitRepository extends JpaRepository<OrderItemUnit, Long> {

    List<OrderItemUnit> findByOrderItemIdIn(Collection<Long> orderItemIds);
}
