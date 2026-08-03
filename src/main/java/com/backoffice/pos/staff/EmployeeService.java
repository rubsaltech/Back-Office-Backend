package com.backoffice.pos.staff;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.staff.dto.EmployeeRequest;
import com.backoffice.pos.staff.dto.EmployeeResponse;
import com.backoffice.pos.store.StoreRepository;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final StoreRepository stores;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employees, StoreRepository stores, RoleRepository roles,
                           PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.stores = stores;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Employee> page = StringUtils.hasText(query)
                ? employees.findByBusinessIdAndFullNameContainingIgnoreCase(businessId, query, pageable)
                : employees.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, EmployeeResponse::from);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (employees.existsByBusinessIdAndEmailIgnoreCase(businessId, req.email())) {
            throw new ConflictException("An employee with this email already exists");
        }
        Employee e = new Employee();
        e.setBusinessId(businessId);
        e.setEmail(req.email());
        apply(e, req);
        if (StringUtils.hasText(req.password())) {
            e.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (StringUtils.hasText(req.pin())) {
            e.setPinHash(passwordEncoder.encode(req.pin()));
        }
        return EmployeeResponse.from(employees.save(e));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee e = load(id);
        e.setEmail(req.email());
        apply(e, req);
        if (StringUtils.hasText(req.password())) {
            e.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (StringUtils.hasText(req.pin())) {
            e.setPinHash(passwordEncoder.encode(req.pin()));
        }
        return EmployeeResponse.from(employees.save(e));
    }

    @Transactional
    public void delete(Long id) {
        employees.delete(load(id));
    }

    private Employee load(Long id) {
        return employees.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Employee", id));
    }

    private void apply(Employee e, EmployeeRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        e.setFullName(req.fullName());
        if (req.status() != null) {
            e.setStatus(req.status());
        }
        e.setStore(req.storeId() == null ? null
                : stores.findByIdAndBusinessId(req.storeId(), businessId)
                .orElseThrow(() -> NotFoundException.of("Store", req.storeId())));
        e.setRole(req.roleId() == null ? null
                : roles.findByIdAndBusinessId(req.roleId(), businessId)
                .orElseThrow(() -> NotFoundException.of("Role", req.roleId())));
    }
}
