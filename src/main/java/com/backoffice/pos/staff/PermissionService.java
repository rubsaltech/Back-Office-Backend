package com.backoffice.pos.staff;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.staff.dto.PermissionRequest;
import com.backoffice.pos.staff.dto.PermissionResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PermissionService {

    private final PermissionRepository permissions;

    public PermissionService(PermissionRepository permissions) {
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Permission> page = StringUtils.hasText(query)
                ? permissions.findByBusinessIdAndKeyContainingIgnoreCase(businessId, query, pageable)
                : permissions.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, PermissionResponse::from);
    }

    @Transactional
    public PermissionResponse create(PermissionRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (permissions.existsByBusinessIdAndKey(businessId, req.key())) {
            throw new ConflictException("Permission already exists: " + req.key());
        }
        Permission p = new Permission();
        p.setBusinessId(businessId);
        p.setKey(req.key());
        p.setDescription(req.description());
        return PermissionResponse.from(permissions.save(p));
    }

    @Transactional
    public PermissionResponse update(Long id, PermissionRequest req) {
        Permission p = load(id);
        p.setKey(req.key());
        p.setDescription(req.description());
        return PermissionResponse.from(permissions.save(p));
    }

    @Transactional
    public void delete(Long id) {
        permissions.delete(load(id));
    }

    private Permission load(Long id) {
        return permissions.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Permission", id));
    }
}
