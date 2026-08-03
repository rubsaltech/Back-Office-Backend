package com.backoffice.pos.floor;

import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.floor.dto.FloorDtos.TableRequest;
import com.backoffice.pos.floor.dto.FloorDtos.TableResponse;
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
@RequestMapping("/api/v1/tables")
public class TableController {

    private final FloorPlanService service;

    public TableController(FloorPlanService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('table.view')")
    public PageResponse<TableResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long floorId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.listTables(query, floorId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('table.create')")
    public TableResponse create(@Valid @RequestBody TableRequest request) {
        return service.createTable(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('table.edit')")
    public TableResponse update(@PathVariable Long id, @Valid @RequestBody TableRequest request) {
        return service.updateTable(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('table.delete')")
    public void delete(@PathVariable Long id) {
        service.deleteTable(id);
    }
}
