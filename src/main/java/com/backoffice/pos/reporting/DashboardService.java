package com.backoffice.pos.reporting;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.reporting.DashboardSummary.EmployeeOverview;
import com.backoffice.pos.reporting.DashboardSummary.TopProduct;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.store.StoreResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository products;
    private final EmployeeRepository employees;
    private final StoreResolver storeResolver;

    public DashboardService(ProductRepository products, EmployeeRepository employees, StoreResolver storeResolver) {
        this.products = products;
        this.employees = employees;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Long storeId = storeResolver.currentStoreId();

        List<TopProduct> top = products.findTop7ByStoreIdOrderByQuantitySoldDesc(storeId).stream()
                .map(p -> new TopProduct(p.getName(), p.getQuantitySold()))
                .toList();

        List<EmployeeOverview> overview = employees.findTop6ByStores_IdOrderBySalesTotalDesc(storeId).stream()
                .map(e -> new EmployeeOverview(String.valueOf(e.getId()), e.getFullName(), e.getEmail(),
                        e.getSalesTotal(), e.getTipsTotal()))
                .toList();

        return new DashboardSummary(
                products.countByStoreId(storeId),
                products.countByStoreIdAndStatus(storeId, CatalogStatus.ACTIVE),
                products.sumQuantitySoldByStore(storeId),
                employees.countByStores_Id(storeId),
                top,
                overview
        );
    }
}
