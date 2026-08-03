package com.backoffice.pos.reporting;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(
        long totalItems,
        long activeItems,
        long itemsSold,
        long totalEmployees,
        List<TopProduct> topSellingProducts,
        List<EmployeeOverview> employeesOverview
) {
    public record TopProduct(String name, long sold) {
    }

    public record EmployeeOverview(String id, String name, String email, BigDecimal sales, BigDecimal tips) {
    }
}
