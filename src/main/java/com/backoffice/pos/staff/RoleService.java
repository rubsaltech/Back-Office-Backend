package com.backoffice.pos.staff;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.staff.dto.RoleRequest;
import com.backoffice.pos.staff.dto.RoleResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public RoleService(RoleRepository roles, PermissionRepository permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Role> page = StringUtils.hasText(query)
                ? roles.findByBusinessIdAndNameContainingIgnoreCase(businessId, query, pageable)
                : roles.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, RoleResponse::from);
    }

    @Transactional
    public RoleResponse create(RoleRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (roles.existsByBusinessIdAndName(businessId, req.name())) {
            throw new ConflictException("Role already exists: " + req.name());
        }
        Role role = new Role();
        role.setBusinessId(businessId);
        apply(role, req);
        return RoleResponse.from(roles.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest req) {
        Role role = load(id);
        apply(role, req);
        return RoleResponse.from(roles.save(role));
    }

    @Transactional
    public void delete(Long id) {
        roles.delete(load(id));
    }

    private Role load(Long id) {
        return roles.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Role", id));
    }

    private void apply(Role role, RoleRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        role.setName(req.name());
        role.setDescription(req.description());
        Set<Permission> resolved = new HashSet<>();
        if (req.permissionIds() != null) {
            for (Long pid : req.permissionIds()) {
                permissions.findByIdAndBusinessId(pid, businessId).ifPresent(resolved::add);
            }
        }
        role.setPermissions(resolved);
    }
}
