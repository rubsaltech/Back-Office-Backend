package com.backoffice.pos.servicecatalog;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.servicecatalog.dto.ServiceItemRequest;
import com.backoffice.pos.servicecatalog.dto.ServiceItemResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceItemController {

    private final ServiceItemService service;

    public ServiceItemController(ServiceItemService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('service.view')")
    public PageResponse<ServiceItemResponse> list(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return service.list(query, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('service.view')")
    public ServiceItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('service.create')")
    public ServiceItemResponse create(@Valid @RequestBody ServiceItemRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('service.edit')")
    public ServiceItemResponse update(@PathVariable Long id, @Valid @RequestBody ServiceItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('service.delete')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
