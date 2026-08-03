package com.backoffice.pos.floor;

import com.backoffice.pos.floor.dto.FloorDtos.FloorRequest;
import com.backoffice.pos.floor.dto.FloorDtos.FloorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/floors")
public class FloorController {

    private final FloorPlanService service;

    public FloorController(FloorPlanService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('floor.view')")
    public List<FloorResponse> list() {
        return service.listFloors();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('floor.create')")
    public FloorResponse create(@Valid @RequestBody FloorRequest request) {
        return service.createFloor(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('floor.edit')")
    public FloorResponse update(@PathVariable Long id, @Valid @RequestBody FloorRequest request) {
        return service.updateFloor(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('floor.delete')")
    public void delete(@PathVariable Long id) {
        service.deleteFloor(id);
    }
}
