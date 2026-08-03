package com.backoffice.pos.config;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessProvisioningService;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.business.BusinessStatus;
import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.staff.Role;
import com.backoffice.pos.staff.RoleRepository;
import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seeds a demo business/owner/cashier on first run so the app is usable immediately. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BusinessRepository businesses;
    private final BusinessProvisioningService provisioning;
    private final StoreRepository stores;
    private final RoleRepository roles;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(BusinessRepository businesses, BusinessProvisioningService provisioning, StoreRepository stores,
                      RoleRepository roles, EmployeeRepository employees, PasswordEncoder passwordEncoder) {
        this.businesses = businesses;
        this.provisioning = provisioning;
        this.stores = stores;
        this.roles = roles;
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (businesses.count() > 0) {
            return;
        }

        Business business = new Business();
        business.setName("Rubsal Store");
        business.setEmail("owner@rubsal.test");
        business.setPasswordHash(passwordEncoder.encode("password123"));
        business.setOwnerName("Okasha Sipra");
        business.setOwnerPhone("+92 3098487880");
        business.setCurrency("EUR");
        business.setStatus(BusinessStatus.APPROVED);
        business = businesses.save(business);

        provisioning.provisionDefaults(business);

        Store mainStore = stores.findByBusinessIdOrderByCreatedAtAsc(business.getId())
                .stream().findFirst().orElse(null);
        Role cashierRole = roles.findByBusinessIdAndName(business.getId(), "Cashier").orElse(null);

        Employee cashier = new Employee();
        cashier.setBusinessId(business.getId());
        cashier.setFullName("Joe Cashier");
        cashier.setEmail("cashier@rubsal.test");
        cashier.setPasswordHash(passwordEncoder.encode("password123"));
        cashier.setPinHash(passwordEncoder.encode("1234"));
        cashier.setStore(mainStore);
        cashier.setRole(cashierRole);
        employees.save(cashier);

        log.info("""
                ┌───────────────────────────────────────────────────────────┐
                │  RUBSAL POS — demo data seeded                            │
                │  Owner login : owner@rubsal.test / password123            │
                │  Cashier PIN : 1234  (store id: {}, business id: {})       │
                └───────────────────────────────────────────────────────────┘""",
                mainStore != null ? mainStore.getId() : "?", business.getId());
    }
}
