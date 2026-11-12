package com.backoffice.pos.reporting;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.order.OrderRepository;
import com.backoffice.pos.order.OrderStatus;
import com.backoffice.pos.reporting.DashboardSummary.EmployeeOverview;
import com.backoffice.pos.reporting.DashboardSummary.TopProduct;
import com.backoffice.pos.reporting.DashboardSummary.TrendPoint;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.store.StoreResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE");

    private final ProductRepository products;
    private final EmployeeRepository employees;
    private final OrderRepository orders;
    private final StoreResolver storeResolver;

    public DashboardService(ProductRepository products, EmployeeRepository employees, OrderRepository orders,
                            StoreResolver storeResolver) {
        this.products = products;
        this.employees = employees;
        this.orders = orders;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Long storeId = storeResolver.currentStoreId();
        OrderStatus voided = OrderStatus.VOID;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        List<TopProduct> top = products.findTop7ByStoreIdOrderByQuantitySoldDesc(storeId).stream()
                .map(p -> new TopProduct(p.getName(), p.getQuantitySold()))
                .toList();

        List<EmployeeOverview> overview = employees.findTop6ByStores_IdOrderBySalesTotalDesc(storeId).stream()
                .map(e -> new EmployeeOverview(String.valueOf(e.getId()), e.getFullName(), e.getEmail(),
                        e.getSalesTotal(), e.getTipsTotal()))
                .toList();

        // Sales for each of the last 7 days (oldest → today).
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Instant from = d.atStartOfDay(zone).toInstant();
            Instant to = d.plusDays(1).atStartOfDay(zone).toInstant();
            trend.add(new TrendPoint(d.format(DAY), orders.sumTotalByStoreBetween(storeId, voided, from, to)));
        }

        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant();

        return new DashboardSummary(
                products.countByStoreId(storeId),
                products.countByStoreIdAndStatus(storeId, CatalogStatus.ACTIVE),
                products.sumQuantitySoldByStore(storeId),
                employees.countByStores_Id(storeId),
                orders.sumTotalByStoreBetween(storeId, voided, todayStart, tomorrowStart),
                orders.countByStoreBetween(storeId, voided, todayStart, tomorrowStart),
                orders.sumTotalByStore(storeId, voided),
                orders.countByStoreIdAndStatusNot(storeId, voided),
                trend,
                top,
                overview
        );
    }
}
