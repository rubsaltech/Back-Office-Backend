package com.backoffice.pos.config;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessProvisioningService;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.business.BusinessStatus;
import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.staff.Role;
import com.backoffice.pos.order.PaymentDevice;
import com.backoffice.pos.order.PaymentDeviceRepository;
import com.backoffice.pos.staff.RoleRepository;
import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreRepository;
import com.backoffice.pos.store.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds a demo business/owner/cashier on first run so the app is usable immediately.
 * Enabled by default (dev). Set {@code app.seed.demo=false} (env {@code APP_SEED_DEMO=false})
 * in production so it never creates the demo tenant.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BusinessRepository businesses;
    private final BusinessProvisioningService provisioning;
    private final StoreRepository stores;
    private final RoleRepository roles;
    private final EmployeeRepository employees;
    private final PaymentDeviceRepository paymentDevices;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(BusinessRepository businesses, BusinessProvisioningService provisioning, StoreRepository stores,
                      RoleRepository roles, EmployeeRepository employees, PaymentDeviceRepository paymentDevices,
                      PasswordEncoder passwordEncoder) {
        this.businesses = businesses;
        this.provisioning = provisioning;
        this.stores = stores;
        this.roles = roles;
        this.employees = employees;
        this.paymentDevices = paymentDevices;
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

        // Demo store (restaurant vertical) + its default card terminal.
        Store mainStore = new Store();
        mainStore.setBusinessId(business.getId());
        mainStore.setName("Main Store");
        mainStore.setType(StoreType.RESTAURANT);
        mainStore.setMain(true);
        mainStore = stores.save(mainStore);

        PaymentDevice device = new PaymentDevice();
        device.setBusinessId(business.getId());
        device.setStoreId(mainStore.getId());
        device.setSerialNumber("0821595192");
        device.setLabel("Main Terminal");
        paymentDevices.save(device);

        Role cashierRole = roles.findByBusinessIdAndName(business.getId(), "Cashier").orElse(null);

        Employee cashier = new Employee();
        cashier.setBusinessId(business.getId());
        cashier.setFullName("Joe Cashier");
        cashier.setEmail("cashier@rubsal.test");
        cashier.setPasswordHash(passwordEncoder.encode("password123"));
        cashier.setPinHash(passwordEncoder.encode("1234"));
        cashier.getStores().add(mainStore);
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
