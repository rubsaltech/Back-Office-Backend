package com.backoffice.pos.servicejob;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.order.dto.OrderResponse;
import com.backoffice.pos.servicejob.dto.AddServiceJobChargeRequest;
import com.backoffice.pos.servicejob.dto.AddServiceJobProductRequest;
import com.backoffice.pos.servicejob.dto.AddServiceUpdateRequest;
import com.backoffice.pos.servicejob.dto.BillServiceJobRequest;
import com.backoffice.pos.servicejob.dto.CreateServiceJobRequest;
import com.backoffice.pos.servicejob.dto.ServiceJobResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Service/repair job tracking. Reuses the service.* permissions (billing uses order.create). */
@RestController
@RequestMapping("/api/v1/service-jobs")
public class ServiceJobController {

    private final ServiceJobService service;

    public ServiceJobController(ServiceJobService service) {
        this.service = service;
    }

    /** Start a new job for a chosen service → returns it with its token. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse create(@Valid @RequestBody CreateServiceJobRequest request) {
        return service.create(request);
    }

    /** Look a job up by its customer token and get its full detail. */
    @GetMapping("/track")
    @PreAuthorize("hasAuthority('service.view')")
    public ServiceJobResponse track(@RequestParam String token) {
        return service.track(token);
    }

    /** Record a progress update (advance the stage). */
    @PostMapping("/{id}/updates")
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse addUpdate(@PathVariable Long id, @Valid @RequestBody AddServiceUpdateRequest request) {
        return service.addUpdate(id, request);
    }

    /** Add a part consumed by the job (draws stock down). */
    @PostMapping("/{id}/products")
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse addProduct(@PathVariable Long id, @Valid @RequestBody AddServiceJobProductRequest request) {
        return service.addProduct(id, request);
    }

    /** Remove a consumed part from the job (returns its stock). */
    @DeleteMapping("/{id}/products/{lineId}")
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse removeProduct(@PathVariable Long id, @PathVariable Long lineId) {
        return service.removeProduct(id, lineId);
    }

    /** Add an extra charge to the job (beyond the base charge). */
    @PostMapping("/{id}/charges")
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse addCharge(@PathVariable Long id, @Valid @RequestBody AddServiceJobChargeRequest request) {
        return service.addCharge(id, request);
    }

    /** Remove an extra charge from the job. */
    @DeleteMapping("/{id}/charges/{chargeId}")
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceJobResponse removeCharge(@PathVariable Long id, @PathVariable Long chargeId) {
        return service.removeCharge(id, chargeId);
    }

    /** Bill a job at the POS → creates the order and closes the job; returns the order to print. */
    @PostMapping("/bill")
    @PreAuthorize("hasAuthority('order.create')")
    public OrderResponse bill(@Valid @RequestBody BillServiceJobRequest request) {
        return service.bill(request);
    }

    /** List the running (open) jobs for the active store. */
    @GetMapping
    @PreAuthorize("hasAuthority('service.view')")
    public PageResponse<ServiceJobResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listOpen(pageable);
    }
}
