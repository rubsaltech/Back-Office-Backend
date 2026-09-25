package com.backoffice.pos.order;

import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceRequest;
import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-devices")
public class PaymentDeviceController {

    private final PaymentDeviceService service;

    public PaymentDeviceController(PaymentDeviceService service) {
        this.service = service;
    }

    /** Listed at Card payment. Gated by order.pay (cashiers can take payment). */
    @GetMapping
    @PreAuthorize("hasAuthority('order.pay')")
    public List<PaymentDeviceResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('store.create')")
    public PaymentDeviceResponse create(@Valid @RequestBody PaymentDeviceRequest request) {
        return service.create(request);
    }
}
