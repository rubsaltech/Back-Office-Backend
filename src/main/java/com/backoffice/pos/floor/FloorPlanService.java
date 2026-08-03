package com.backoffice.pos.floor;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.floor.dto.FloorDtos.FloorRequest;
import com.backoffice.pos.floor.dto.FloorDtos.FloorResponse;
import com.backoffice.pos.floor.dto.FloorDtos.TableRequest;
import com.backoffice.pos.floor.dto.FloorDtos.TableResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FloorPlanService {

    private final FloorRepository floors;
    private final TableRepository tables;

    public FloorPlanService(FloorRepository floors, TableRepository tables) {
        this.floors = floors;
        this.tables = tables;
    }

    // ---- Floors ----

    @Transactional(readOnly = true)
    public List<FloorResponse> listFloors() {
        return floors.findByBusinessIdOrderByNameAsc(TenantContext.requireBusinessId())
                .stream().map(f -> FloorResponse.from(f, tables.countByFloor_Id(f.getId()))).toList();
    }

    @Transactional
    public FloorResponse createFloor(FloorRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (floors.existsByBusinessIdAndName(businessId, req.name())) {
            throw new ConflictException("Floor already exists: " + req.name());
        }
        Floor f = new Floor();
        f.setBusinessId(businessId);
        f.setName(req.name());
        return FloorResponse.from(floors.save(f), 0);
    }

    @Transactional
    public FloorResponse updateFloor(Long id, FloorRequest req) {
        Floor f = loadFloor(id);
        f.setName(req.name());
        return FloorResponse.from(floors.save(f), tables.countByFloor_Id(f.getId()));
    }

    @Transactional
    public void deleteFloor(Long id) {
        floors.delete(loadFloor(id));
    }

    // ---- Tables ----

    @Transactional(readOnly = true)
    public PageResponse<TableResponse> listTables(String query, Long floorId, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<RestaurantTable> page;
        if (floorId != null) {
            page = tables.findByBusinessIdAndFloor_Id(businessId, floorId, pageable);
        } else if (StringUtils.hasText(query)) {
            page = tables.findByBusinessIdAndNameContainingIgnoreCase(businessId, query, pageable);
        } else {
            page = tables.findByBusinessId(businessId, pageable);
        }
        return PageResponse.of(page, TableResponse::from);
    }

    @Transactional
    public TableResponse createTable(TableRequest req) {
        RestaurantTable t = new RestaurantTable();
        t.setBusinessId(TenantContext.requireBusinessId());
        applyTable(t, req);
        return TableResponse.from(tables.save(t));
    }

    @Transactional
    public TableResponse updateTable(Long id, TableRequest req) {
        RestaurantTable t = loadTable(id);
        applyTable(t, req);
        return TableResponse.from(tables.save(t));
    }

    @Transactional
    public void deleteTable(Long id) {
        tables.delete(loadTable(id));
    }

    // ---- helpers ----

    private Floor loadFloor(Long id) {
        return floors.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Floor", id));
    }

    private RestaurantTable loadTable(Long id) {
        return tables.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Table", id));
    }

    private void applyTable(RestaurantTable t, TableRequest req) {
        t.setName(req.name());
        t.setSeats(req.seats());
        if (req.status() != null) {
            t.setStatus(req.status());
        }
        t.setFloor(req.floorId() == null ? null
                : floors.findByIdAndBusinessId(req.floorId(), TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Floor", req.floorId())));
    }
}
