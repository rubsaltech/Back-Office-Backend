package com.backoffice.pos.staff;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.staff.dto.EmployeeRequest;
import com.backoffice.pos.staff.dto.EmployeeResponse;
import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreRepository;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final StoreRepository stores;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final StoreResolver storeResolver;

    public EmployeeService(EmployeeRepository employees, StoreRepository stores, RoleRepository roles,
                           PasswordEncoder passwordEncoder, StoreResolver storeResolver) {
        this.employees = employees;
        this.stores = stores;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String query, Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Employee> page = StringUtils.hasText(query)
                ? employees.findByStores_IdAndFullNameContainingIgnoreCase(storeId, query, pageable)
                : employees.findByStores_Id(storeId, pageable);
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
        e.getStores().clear();
        e.getStores().addAll(resolveStores(req, businessId));
        e.setRole(req.roleId() == null ? null
                : roles.findByIdAndBusinessId(req.roleId(), businessId)
                .orElseThrow(() -> NotFoundException.of("Role", req.roleId())));
    }

    /**
     * Resolves the stores an employee works in. Each must belong to the business.
     * When none are supplied, the employee is assigned to the current active store
     * so they are not left unreachable from any store-scoped list.
     */
    private Set<Store> resolveStores(EmployeeRequest req, Long businessId) {
        Set<Store> result = new LinkedHashSet<>();
        if (req.storeIds() != null) {
            for (Long storeId : req.storeIds()) {
                if (storeId == null) {
                    continue;
                }
                result.add(stores.findByIdAndBusinessId(storeId, businessId)
                        .orElseThrow(() -> NotFoundException.of("Store", storeId)));
            }
        }
        if (result.isEmpty()) {
            Long active = storeResolver.currentStoreId();
            stores.findByIdAndBusinessId(active, businessId).ifPresent(result::add);
        }
        return result;
    }
}
