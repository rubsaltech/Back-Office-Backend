package com.backoffice.pos.order;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.floor.RestaurantTable;
import com.backoffice.pos.floor.TableRepository;
import com.backoffice.pos.order.dto.OrderDtos.OrderSummary;
import com.backoffice.pos.order.dto.OrderRequest;
import com.backoffice.pos.order.dto.OrderResponse;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderService {

    private static final long ORDER_NUMBER_BASE = 1000L;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final TableRepository tables;
    private final PaymentDeviceRepository devices;

    public OrderService(OrderRepository orders, ProductRepository products, TableRepository tables,
                        PaymentDeviceRepository devices) {
        this.orders = orders;
        this.products = products;
        this.tables = tables;
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(OrderStatus status, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Order> page = status != null
                ? orders.findByBusinessIdAndStatus(businessId, status, pageable)
                : orders.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return OrderResponse.from(load(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest req) {
        Long businessId = TenantContext.requireBusinessId();

        Order order = new Order();
        order.setBusinessId(businessId);
        order.setOrderNumber(nextOrderNumber(businessId));
        order.setType(req.type());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setGuestCount(req.guestCount() == null || req.guestCount() < 1 ? 1 : req.guestCount());
        order.setKitchenNote(req.kitchenNote());
        order.setHandlerName(safeHandlerName());

        // A table is attached whenever one is supplied (dine-in verticals only);
        // customer details are stored whenever supplied (delivery/repair/service).
        // The store's vertical decides which the POS collects — the backend just
        // records what it is given.
        if (req.tableId() != null) {
            RestaurantTable table = tables.findByIdAndBusinessId(req.tableId(), businessId)
                    .orElseThrow(() -> NotFoundException.of("Table", req.tableId()));
            order.setTableId(table.getId());
            order.setTableName(table.getName());
        }
        if (StringUtils.hasText(req.customerName()) || StringUtils.hasText(req.customerAddress())
                || StringUtils.hasText(req.customerPhone())) {
            order.setCustomerName(req.customerName());
            order.setCustomerPhone(req.customerPhone());
            order.setCustomerAddress(req.customerAddress());
        }

        buildItems(order, req, businessId);
        applyTotals(order, req);
        attachPayment(order, req, businessId);

        return OrderResponse.from(orders.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = load(id);
        order.setStatus(status);
        return OrderResponse.from(orders.save(order));
    }

    @Transactional
    public OrderResponse voidOrder(Long id) {
        Order order = load(id);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "A completed order cannot be voided");
        }
        order.setStatus(OrderStatus.VOID);
        return OrderResponse.from(orders.save(order));
    }

    // ---- helpers ----

    private Order load(Long id) {
        return orders.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Order", id));
    }

    private long nextOrderNumber(Long businessId) {
        return orders.findTopByBusinessIdOrderByOrderNumberDesc(businessId)
                .map(o -> o.getOrderNumber() + 1)
                .orElse(ORDER_NUMBER_BASE + 1);
    }

    private void buildItems(Order order, OrderRequest req, Long businessId) {
        int sort = 0;
        for (OrderRequest.Line line : req.items()) {
            if (line == null || line.productId() == null) {
                continue;
            }
            Product product = products.findByIdAndBusinessId(line.productId(), businessId)
                    .orElseThrow(() -> NotFoundException.of("Product", line.productId()));

            int qty = line.quantity() == null || line.quantity() < 1 ? 1 : line.quantity();

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSeatNumber(line.seatNumber());
            item.setQuantity(qty);
            item.setSpecialInstructions(line.specialInstructions());
            item.setSortOrder(sort++);

            BigDecimal unitPrice = nvl(product.getPrice());
            item.setUnitPrice(unitPrice);
            item.setTaxAmount(nvl(product.getTaxAmount()).multiply(BigDecimal.valueOf(qty)));
            item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
            order.addItem(item);
        }
        if (order.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An order must contain at least one item");
        }
    }

    private void applyTotals(Order order, OrderRequest req) {
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTotal = order.getItems().stream()
                .map(OrderItem::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = BigDecimal.ZERO;
        if (req.discountType() != null && req.discountValue() != null
                && req.discountValue().compareTo(BigDecimal.ZERO) > 0) {
            order.setDiscountType(req.discountType());
            order.setDiscountValue(req.discountValue());
            discountTotal = req.discountType() == DiscountType.PERCENTAGE
                    ? subtotal.multiply(req.discountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : req.discountValue();
            // Never discount below zero.
            if (discountTotal.compareTo(subtotal) > 0) {
                discountTotal = subtotal;
            }
        }

        order.setSubtotal(subtotal);
        order.setTaxTotal(taxTotal);
        order.setDiscountTotal(discountTotal);
        order.setTotal(subtotal.add(taxTotal).subtract(discountTotal));
    }

    private void attachPayment(Order order, OrderRequest req, Long businessId) {
        OrderRequest.Payment p = req.payment();
        if (p == null || p.method() == null) {
            return;
        }
        Payment payment = new Payment();
        payment.setMethod(p.method());
        payment.setAmount(order.getTotal());
        payment.setStatus(p.method() == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.SUCCESSFUL);
        if (p.method() == PaymentMethod.CARD && p.deviceId() != null) {
            PaymentDevice device = devices.findByIdAndBusinessId(p.deviceId(), businessId)
                    .orElseThrow(() -> NotFoundException.of("Payment device", p.deviceId()));
            payment.setDeviceId(device.getId());
            payment.setDeviceSerial(device.getSerialNumber());
        }
        order.addPayment(payment);
    }

    private String safeHandlerName() {
        try {
            return CurrentUser.get().displayName();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
