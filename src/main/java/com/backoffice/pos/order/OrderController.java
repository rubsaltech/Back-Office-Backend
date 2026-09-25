package com.backoffice.pos.order;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.order.dto.OrderDtos.OrderSummary;
import com.backoffice.pos.order.dto.OrderDtos.StatusUpdate;
import com.backoffice.pos.order.dto.OrderRequest;
import com.backoffice.pos.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.view')")
    public PageResponse<OrderSummary> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.view')")
    public OrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('order.create')")
    public OrderResponse create(@Valid @RequestBody OrderRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order.view')")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate request) {
        return service.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('order.void')")
    public OrderResponse voidOrder(@PathVariable Long id) {
        return service.voidOrder(id);
    }
}
