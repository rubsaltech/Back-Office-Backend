package com.backoffice.pos.servicejob;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.catalog.ProductVariantRepository;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.inventory.StockService;
import com.backoffice.pos.order.OrderService;
import com.backoffice.pos.order.dto.OrderResponse;
import com.backoffice.pos.order.dto.ServiceBillRequest;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.servicecatalog.ServiceItem;
import com.backoffice.pos.servicecatalog.ServiceItemRepository;
import com.backoffice.pos.servicejob.dto.AddServiceJobChargeRequest;
import com.backoffice.pos.servicejob.dto.AddServiceJobProductRequest;
import com.backoffice.pos.servicejob.dto.AddServiceUpdateRequest;
import com.backoffice.pos.servicejob.dto.BillServiceJobRequest;
import com.backoffice.pos.servicejob.dto.CreateServiceJobRequest;
import com.backoffice.pos.servicejob.dto.ServiceJobResponse;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ServiceJobService {

    // Unambiguous alphabet (no 0/O/1/I) for customer-friendly tokens.
    private static final char[] TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TOKEN_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Statuses that count as "running" (shown in the track list; still billable). */
    private static final Set<ServiceJobStatus> OPEN_STATUSES = EnumSet.of(
            ServiceJobStatus.RECEIVED, ServiceJobStatus.DIAGNOSED,
            ServiceJobStatus.IN_PROGRESS, ServiceJobStatus.READY);

    private final ServiceJobRepository jobs;
    private final ServiceJobUpdateRepository updates;
    private final ServiceJobProductRepository jobProducts;
    private final ServiceJobChargeRepository jobCharges;
    private final ServiceItemRepository serviceItems;
    private final ProductVariantRepository variants;
    private final StockService stockService;
    private final OrderService orderService;
    private final StoreResolver storeResolver;

    public ServiceJobService(ServiceJobRepository jobs, ServiceJobUpdateRepository updates,
                             ServiceJobProductRepository jobProducts, ServiceJobChargeRepository jobCharges,
                             ServiceItemRepository serviceItems,
                             ProductVariantRepository variants, StockService stockService,
                             OrderService orderService, StoreResolver storeResolver) {
        this.jobs = jobs;
        this.updates = updates;
        this.jobProducts = jobProducts;
        this.jobCharges = jobCharges;
        this.serviceItems = serviceItems;
        this.variants = variants;
        this.stockService = stockService;
        this.orderService = orderService;
        this.storeResolver = storeResolver;
    }

    @Transactional
    public ServiceJobResponse create(CreateServiceJobRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        String handler = currentHandler();

        ServiceItem svc = serviceItems.findByIdAndStoreId(req.serviceId(), storeId)
                .orElseThrow(() -> NotFoundException.of("Service", req.serviceId()));

        ServiceJob job = new ServiceJob();
        job.setBusinessId(businessId);
        job.setStoreId(storeId);
        job.setToken(generateToken(businessId));
        job.setCustomerName(req.customerName());
        job.setCustomerPhone(req.customerPhone());
        job.setDevice(req.device());
        job.setServiceId(svc.getId());
        job.setServiceType(svc.getName());
        job.setServiceCharge(svc.getPrice() == null ? BigDecimal.ZERO : svc.getPrice());
        job.setStatus(ServiceJobStatus.RECEIVED);
        job.setHandlerName(handler);
        job = jobs.save(job);

        addTimeline(job.getId(), ServiceJobStatus.RECEIVED,
                StringUtils.hasText(req.note()) ? req.note() : "Service request received", handler);

        return full(job);
    }

    @Transactional(readOnly = true)
    public ServiceJobResponse track(String token) {
        Long businessId = TenantContext.requireBusinessId();
        ServiceJob job = jobs.findByBusinessIdAndTokenIgnoreCase(businessId, token.trim())
                .orElseThrow(() -> new NotFoundException("No service found for token: " + token));
        return full(job);
    }

    @Transactional
    public ServiceJobResponse addUpdate(Long jobId, AddServiceUpdateRequest req) {
        ServiceJob job = loadForStore(jobId);
        addTimeline(job.getId(), req.status(), req.note(), currentHandler());
        job.setStatus(req.status());
        jobs.save(job);
        return full(job);
    }

    /** Add a consumed part to a job — draws stock down immediately. */
    @Transactional
    public ServiceJobResponse addProduct(Long jobId, AddServiceJobProductRequest req) {
        ServiceJob job = loadForStore(jobId);
        requireNotBilled(job);
        int qty = req.quantity() == null || req.quantity() < 1 ? 1 : req.quantity();
        ProductVariant v = variants.findByIdAndStoreId(req.variantId(), job.getStoreId())
                .orElseThrow(() -> NotFoundException.of("Variant", req.variantId()));

        stockService.consumeForService(v, qty, "Service " + job.getToken());

        Product product = v.getProduct();
        ServiceJobProduct line = new ServiceJobProduct();
        line.setServiceJobId(job.getId());
        line.setProductId(product != null ? product.getId() : null);
        line.setVariantId(v.getId());
        line.setProductName(product != null ? product.getName() : v.getVariantName());
        line.setVariantName(v.isDefault() ? null : v.getVariantName());
        line.setQuantity(qty);
        line.setUnitPrice(v.getPrice() == null ? BigDecimal.ZERO : v.getPrice());
        jobProducts.save(line);

        return full(job);
    }

    /** Add a free-form extra charge to a job (e.g. extra labour). */
    @Transactional
    public ServiceJobResponse addCharge(Long jobId, AddServiceJobChargeRequest req) {
        ServiceJob job = loadForStore(jobId);
        requireNotBilled(job);
        ServiceJobCharge charge = new ServiceJobCharge();
        charge.setServiceJobId(job.getId());
        charge.setTitle(req.title().trim());
        charge.setAmount(req.amount());
        jobCharges.save(charge);
        return full(job);
    }

    /** Remove an extra charge from a job. */
    @Transactional
    public ServiceJobResponse removeCharge(Long jobId, Long chargeId) {
        ServiceJob job = loadForStore(jobId);
        requireNotBilled(job);
        ServiceJobCharge charge = jobCharges.findByIdAndServiceJobId(chargeId, job.getId())
                .orElseThrow(() -> NotFoundException.of("Service job charge", chargeId));
        jobCharges.delete(charge);
        return full(job);
    }

    /** Remove a consumed part from a job — returns its stock. */
    @Transactional
    public ServiceJobResponse removeProduct(Long jobId, Long lineId) {
        ServiceJob job = loadForStore(jobId);
        requireNotBilled(job);
        ServiceJobProduct line = jobProducts.findByIdAndServiceJobId(lineId, job.getId())
                .orElseThrow(() -> NotFoundException.of("Service job part", lineId));

        if (line.getVariantId() != null) {
            variants.findByIdAndStoreId(line.getVariantId(), job.getStoreId())
                    .ifPresent(v -> stockService.returnFromService(v, line.getQuantity(),
                            "Service " + job.getToken() + " — part removed"));
        }
        jobProducts.delete(line);
        return full(job);
    }

    /** Bill a job at the POS: create the order (charge + parts) and close the job. */
    @Transactional
    public OrderResponse bill(BillServiceJobRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        ServiceJob job = jobs.findByBusinessIdAndTokenIgnoreCase(businessId, req.token().trim())
                .orElseThrow(() -> new NotFoundException("No service found for token: " + req.token()));
        if (!job.getStoreId().equals(storeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This service belongs to a different store");
        }
        requireNotBilled(job);

        List<ServiceJobProduct> parts = jobProducts.findByServiceJobIdOrderByCreatedAtAsc(job.getId());
        List<ServiceBillRequest.BillLine> lines = new java.util.ArrayList<>(parts.stream()
                .map(p -> new ServiceBillRequest.BillLine(
                        p.getProductId(), p.getVariantId(), p.getProductName(), p.getVariantName(),
                        p.getQuantity(), p.getUnitPrice()))
                .toList());
        // Extra charges become single-unit lines on the receipt.
        for (ServiceJobCharge c : jobCharges.findByServiceJobIdOrderByCreatedAtAsc(job.getId())) {
            lines.add(new ServiceBillRequest.BillLine(null, null, c.getTitle(), null, 1, c.getAmount()));
        }

        ServiceBillRequest bill = new ServiceBillRequest(
                job.getServiceType(), job.getServiceCharge(),
                job.getCustomerName(), job.getCustomerPhone(),
                lines, req.paymentMethod(), req.deviceId());

        OrderResponse order = orderService.createServiceBill(bill);

        job.setOrderId(order.id());
        job.setBilledAt(Instant.now());
        job.setStatus(ServiceJobStatus.DELIVERED);
        jobs.save(job);
        addTimeline(job.getId(), ServiceJobStatus.DELIVERED,
                "Billed at POS — order #" + order.orderNumber(), currentHandler());

        return order;
    }

    /** Running jobs for the active store (token search finds completed ones). */
    @Transactional(readOnly = true)
    public PageResponse<ServiceJobResponse> listOpen(Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<ServiceJob> page = jobs.findByStoreIdAndStatusInOrderByCreatedAtDesc(storeId, OPEN_STATUSES, pageable);
        return PageResponse.of(page, ServiceJobResponse::summary);
    }

    // ---- helpers ----

    private ServiceJob loadForStore(Long jobId) {
        Long storeId = storeResolver.currentStoreId();
        return jobs.findByIdAndStoreId(jobId, storeId)
                .orElseThrow(() -> NotFoundException.of("Service job", jobId));
    }

    private void requireNotBilled(ServiceJob job) {
        if (job.getOrderId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This service job has already been billed");
        }
    }

    private ServiceJobResponse full(ServiceJob job) {
        return ServiceJobResponse.from(
                job,
                updates.findByServiceJobIdOrderByCreatedAtAsc(job.getId()),
                jobProducts.findByServiceJobIdOrderByCreatedAtAsc(job.getId()),
                jobCharges.findByServiceJobIdOrderByCreatedAtAsc(job.getId()));
    }

    private void addTimeline(Long jobId, ServiceJobStatus status, String note, String by) {
        ServiceJobUpdate u = new ServiceJobUpdate();
        u.setServiceJobId(jobId);
        u.setStatus(status);
        u.setNote(note);
        u.setPerformedBy(by);
        updates.save(u);
    }

    private String generateToken(Long businessId) {
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
            for (int i = 0; i < TOKEN_LENGTH; i++) {
                sb.append(TOKEN_ALPHABET[RANDOM.nextInt(TOKEN_ALPHABET.length)]);
            }
            String token = sb.toString();
            if (!jobs.existsByBusinessIdAndToken(businessId, token)) {
                return token;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "Could not generate a unique token — please try again");
    }

    private String currentHandler() {
        try {
            return CurrentUser.get().displayName();
        } catch (Exception e) {
            return null;
        }
    }
}
