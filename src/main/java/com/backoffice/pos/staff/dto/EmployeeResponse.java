package com.backoffice.pos.staff.dto;

import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeStatus;

import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        Long storeId,
        String storeName,
        Long roleId,
        String roleName,
        EmployeeStatus status,
        BigDecimal salesTotal,
        BigDecimal tipsTotal
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getFullName(), e.getEmail(),
                e.getStore() != null ? e.getStore().getId() : null,
                e.getStore() != null ? e.getStore().getName() : null,
                e.getRole() != null ? e.getRole().getId() : null,
                e.getRole() != null ? e.getRole().getName() : null,
                e.getStatus(), e.getSalesTotal(), e.getTipsTotal());
    }
}
