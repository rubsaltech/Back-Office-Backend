package com.backoffice.pos.staff;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.staff.dto.PermissionRequest;
import com.backoffice.pos.staff.dto.PermissionResponse;
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
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission.view')")
    public PageResponse<PermissionResponse> list(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 15, sort = "key") Pageable pageable) {
        return service.list(query, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('permission.create')")
    public PermissionResponse create(@Valid @RequestBody PermissionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission.edit')")
    public PermissionResponse update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('permission.delete')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
