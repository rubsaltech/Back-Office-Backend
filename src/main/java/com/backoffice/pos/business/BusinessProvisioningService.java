package com.backoffice.pos.business;

import com.backoffice.pos.staff.DefaultPermissions;
import com.backoffice.pos.staff.Permission;
import com.backoffice.pos.staff.PermissionRepository;
import com.backoffice.pos.staff.Role;
import com.backoffice.pos.staff.RoleRepository;
import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Seeds the default permissions, starter roles and a main store for a new business. */
@Service
public class BusinessProvisioningService {

    private final PermissionRepository permissions;
    private final RoleRepository roles;
    private final StoreRepository stores;

    public BusinessProvisioningService(PermissionRepository permissions, RoleRepository roles, StoreRepository stores) {
        this.permissions = permissions;
        this.roles = roles;
        this.stores = stores;
    }

    @Transactional
    public void provisionDefaults(Business business) {
        Long businessId = business.getId();

        // 1) Permission catalog
        Map<String, Permission> byKey = new HashMap<>();
        DefaultPermissions.CATALOG.forEach((key, description) -> {
            Permission p = new Permission();
            p.setBusinessId(businessId);
            p.setKey(key);
            p.setDescription(description);
            byKey.put(key, permissions.save(p));
        });

        // 2) Starter roles
        DefaultPermissions.ROLE_TEMPLATES.forEach((roleName, keys) -> {
            Role role = new Role();
            role.setBusinessId(businessId);
            role.setName(roleName);
            role.setDescription(roleName + " role");
            Set<Permission> granted = new HashSet<>();
            if (keys.equals(List.of("*"))) {
                granted.addAll(byKey.values());
            } else {
                keys.forEach(k -> {
                    Permission p = byKey.get(k);
                    if (p != null) {
                        granted.add(p);
                    }
                });
            }
            role.setPermissions(granted);
            roles.save(role);
        });

        // 3) Main store
        Store main = new Store();
        main.setBusinessId(businessId);
        main.setName("Main Store");
        main.setMain(true);
        stores.save(main);
    }
}
