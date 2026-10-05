package com.backoffice.pos.staff.dto;

import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeStatus;

import java.math.BigDecimal;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        List<StoreRef> stores,
        Long roleId,
        String roleName,
        EmployeeStatus status,
        BigDecimal salesTotal,
        BigDecimal tipsTotal
) {
    public record StoreRef(Long id, String name) {
    }

    public static EmployeeResponse from(Employee e) {
        List<StoreRef> stores = e.getStores().stream()
                .map(s -> new StoreRef(s.getId(), s.getName()))
                .toList();
        return new EmployeeResponse(
                e.getId(), e.getFullName(), e.getEmail(), stores,
                e.getRole() != null ? e.getRole().getId() : null,
                e.getRole() != null ? e.getRole().getName() : null,
                e.getStatus(), e.getSalesTotal(), e.getTipsTotal());
    }
}
