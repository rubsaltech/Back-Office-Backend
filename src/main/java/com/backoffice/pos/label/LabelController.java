package com.backoffice.pos.label;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.label.dto.LabelRequest;
import com.backoffice.pos.label.dto.LabelResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final LabelService service;

    public LabelController(LabelService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('label.view')")
    public PageResponse<LabelResponse> list(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return service.list(query, pageable);
    }

    /** Unpaged list for dropdowns / attach pickers. */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('label.view')")
    public List<LabelResponse> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('label.view')")
    public LabelResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('label.create')")
    public LabelResponse create(@Valid @RequestBody LabelRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('label.edit')")
    public LabelResponse update(@PathVariable Long id, @Valid @RequestBody LabelRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('label.delete')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
