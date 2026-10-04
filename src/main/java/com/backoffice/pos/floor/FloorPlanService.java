package com.backoffice.pos.floor;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.floor.dto.FloorDtos.FloorRequest;
import com.backoffice.pos.floor.dto.FloorDtos.FloorResponse;
import com.backoffice.pos.floor.dto.FloorDtos.TableRequest;
import com.backoffice.pos.floor.dto.FloorDtos.TableResponse;
import com.backoffice.pos.store.StoreResolver;
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
    private final StoreResolver storeResolver;

    public FloorPlanService(FloorRepository floors, TableRepository tables, StoreResolver storeResolver) {
        this.floors = floors;
        this.tables = tables;
        this.storeResolver = storeResolver;
    }

    // ---- Floors ----

    @Transactional(readOnly = true)
    public List<FloorResponse> listFloors() {
        return floors.findByStoreIdOrderByNameAsc(storeResolver.currentStoreId())
                .stream().map(f -> FloorResponse.from(f, tables.countByFloor_Id(f.getId()))).toList();
    }

    @Transactional
    public FloorResponse createFloor(FloorRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        if (floors.existsByStoreIdAndName(storeId, req.name())) {
            throw new ConflictException("Floor already exists in this store: " + req.name());
        }
        Floor f = new Floor();
        f.setBusinessId(businessId);
        f.setStoreId(storeId);
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
        Long storeId = storeResolver.currentStoreId();
        Page<RestaurantTable> page;
        if (floorId != null) {
            page = tables.findByStoreIdAndFloor_Id(storeId, floorId, pageable);
        } else if (StringUtils.hasText(query)) {
            page = tables.findByStoreIdAndNameContainingIgnoreCase(storeId, query, pageable);
        } else {
            page = tables.findByStoreId(storeId, pageable);
        }
        return PageResponse.of(page, TableResponse::from);
    }

    @Transactional
    public TableResponse createTable(TableRequest req) {
        RestaurantTable t = new RestaurantTable();
        t.setBusinessId(TenantContext.requireBusinessId());
        t.setStoreId(storeResolver.currentStoreId());
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
        return floors.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Floor", id));
    }

    private RestaurantTable loadTable(Long id) {
        return tables.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Table", id));
    }

    private void applyTable(RestaurantTable t, TableRequest req) {
        t.setName(req.name());
        t.setSeats(req.seats());
        if (req.status() != null) {
            t.setStatus(req.status());
        }
        // A table's floor must belong to the same store.
        t.setFloor(req.floorId() == null ? null
                : floors.findByIdAndStoreId(req.floorId(), t.getStoreId())
                .orElseThrow(() -> NotFoundException.of("Floor", req.floorId())));
    }
}
