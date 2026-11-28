package com.backoffice.pos.customer;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.customer.dto.CustomerRequest;
import com.backoffice.pos.customer.dto.CustomerResponse;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerTransactionRepository transactions;
    private final StoreResolver storeResolver;

    public CustomerService(CustomerRepository customers, CustomerTransactionRepository transactions,
                           StoreResolver storeResolver) {
        this.customers = customers;
        this.transactions = transactions;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String query, Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Customer> page = StringUtils.hasText(query)
                ? customers.findByStoreIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(storeId, query.trim(), pageable)
                : customers.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        return PageResponse.of(page, CustomerResponse::summary);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        Customer c = load(id);
        return CustomerResponse.detail(c, transactions.findByCustomerIdOrderByCreatedAtDesc(c.getId()));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        Customer c = new Customer();
        c.setBusinessId(TenantContext.requireBusinessId());
        c.setStoreId(storeResolver.currentStoreId());
        c.setBalance(BigDecimal.ZERO);
        apply(c, req);
        c = customers.save(c);
        return CustomerResponse.detail(c, java.util.List.of());
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = load(id);
        apply(c, req);
        c = customers.save(c);
        return CustomerResponse.detail(c, transactions.findByCustomerIdOrderByCreatedAtDesc(c.getId()));
    }

    @Transactional
    public void delete(Long id) {
        customers.delete(load(id));
    }

    /**
     * Record a POS sale against a customer's ledger. When the customer underpays
     * (received &lt; total), writes a DEBIT for the full total and a CREDIT for the
     * amount received, so their running balance rises by the unpaid amount. A
     * fully-paid sale leaves no ledger entries. Received is capped at the total.
     */
    @Transactional
    public void applyOrderCharge(Long customerId, Long orderId, Long orderNumber, BigDecimal total, BigDecimal received) {
        if (customerId == null) {
            return;
        }
        Long storeId = storeResolver.currentStoreId();
        Customer c = customers.findByIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> NotFoundException.of("Customer", customerId));
        BigDecimal t = nvl(total);
        BigDecimal r = nvl(received);
        if (r.compareTo(t) > 0) {
            r = t; // no overpayment — cap at the total
        }
        BigDecimal balance = t.subtract(r);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return; // fully paid — nothing owed, no ledger entries
        }
        String ref = orderNumber != null ? "#" + orderNumber : "";
        tx(c, CustomerTxnType.DEBIT, t, orderId, "Sale " + ref);
        if (r.compareTo(BigDecimal.ZERO) > 0) {
            tx(c, CustomerTxnType.CREDIT, r, orderId, "Payment " + ref);
        }
        c.setBalance(nvl(c.getBalance()).add(balance));
        customers.save(c);
    }

    private void tx(Customer c, CustomerTxnType type, BigDecimal amount, Long orderId, String note) {
        CustomerTransaction x = new CustomerTransaction();
        x.setBusinessId(c.getBusinessId());
        x.setStoreId(c.getStoreId());
        x.setCustomerId(c.getId());
        x.setType(type);
        x.setAmount(amount);
        x.setOrderId(orderId);
        x.setNote(note.trim());
        transactions.save(x);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ---- helpers ----

    private Customer load(Long id) {
        return customers.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Customer", id));
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setName(req.name().trim());
        c.setEmail(trimToNull(req.email()));
        c.setPhone(trimToNull(req.phone()));
        c.setAddress(trimToNull(req.address()));
        c.setCity(trimToNull(req.city()));
        c.setCountry(trimToNull(req.country()));
        c.setNote(trimToNull(req.note()));
        c.setStatus(req.status() == null ? CatalogStatus.ACTIVE : req.status());
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
