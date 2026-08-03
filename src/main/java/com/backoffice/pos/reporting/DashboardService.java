package com.backoffice.pos.reporting;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.reporting.DashboardSummary.EmployeeOverview;
import com.backoffice.pos.reporting.DashboardSummary.TopProduct;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository products;
    private final EmployeeRepository employees;

    public DashboardService(ProductRepository products, EmployeeRepository employees) {
        this.products = products;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Long businessId = TenantContext.requireBusinessId();

        List<TopProduct> top = products.findTop7ByBusinessIdOrderByQuantitySoldDesc(businessId).stream()
                .map(p -> new TopProduct(p.getName(), p.getQuantitySold()))
                .toList();

        List<EmployeeOverview> overview = employees.findTop6ByBusinessIdOrderBySalesTotalDesc(businessId).stream()
                .map(e -> new EmployeeOverview(String.valueOf(e.getId()), e.getFullName(), e.getEmail(),
                        e.getSalesTotal(), e.getTipsTotal()))
                .toList();

        return new DashboardSummary(
                products.countByBusinessId(businessId),
                products.countByBusinessIdAndStatus(businessId, CatalogStatus.ACTIVE),
                products.sumQuantitySold(businessId),
                employees.countByBusinessId(businessId),
                top,
                overview
        );
    }
}
