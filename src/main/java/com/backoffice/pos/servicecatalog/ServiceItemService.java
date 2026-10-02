package com.backoffice.pos.servicecatalog;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.label.LabelAttachments;
import com.backoffice.pos.servicecatalog.dto.ServiceItemRequest;
import com.backoffice.pos.servicecatalog.dto.ServiceItemResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class ServiceItemService {

    private final ServiceItemRepository services;
    private final ProductRepository products;
    private final LabelAttachments labelAttachments;

    public ServiceItemService(ServiceItemRepository services, ProductRepository products,
                              LabelAttachments labelAttachments) {
        this.services = services;
        this.products = products;
        this.labelAttachments = labelAttachments;
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceItemResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<ServiceItem> page = StringUtils.hasText(query)
                ? services.findByBusinessIdAndNameContainingIgnoreCase(businessId, query, pageable)
                : services.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, ServiceItemResponse::from);
    }

    @Transactional(readOnly = true)
    public ServiceItemResponse get(Long id) {
        return ServiceItemResponse.from(load(id));
    }

    @Transactional
    public ServiceItemResponse create(ServiceItemRequest req) {
        ServiceItem s = new ServiceItem();
        s.setBusinessId(TenantContext.requireBusinessId());
        apply(s, req);
        return ServiceItemResponse.from(services.save(s));
    }

    @Transactional
    public ServiceItemResponse update(Long id, ServiceItemRequest req) {
        ServiceItem s = load(id);
        apply(s, req);
        return ServiceItemResponse.from(services.save(s));
    }

    @Transactional
    public void delete(Long id) {
        services.delete(load(id));
    }

    // ---- helpers ----

    private ServiceItem load(Long id) {
        return services.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Service", id));
    }

    private void apply(ServiceItem s, ServiceItemRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        s.setName(req.name());
        s.setDescription(req.description());
        s.setPrice(nvl(req.price()));
        if (req.status() != null) {
            s.setStatus(req.status());
        }
        rebuildProducts(s, req, businessId);
        rebuildLabels(s, req, businessId);
    }

    private void rebuildLabels(ServiceItem s, ServiceItemRequest req, Long businessId) {
        s.clearLabels();
        for (LabelAttachments.Resolved r : labelAttachments.resolve(req.labels(), businessId)) {
            ServiceItemLabel sl = new ServiceItemLabel();
            sl.setLabelId(r.labelId());
            sl.setLabelName(r.name());
            sl.setLabelType(r.type());
            sl.setSortOrder(r.sortOrder());
            sl.getValues().addAll(r.values());
            s.addLabel(sl);
        }
    }

    private void rebuildProducts(ServiceItem s, ServiceItemRequest req, Long businessId) {
        s.clearProducts();
        if (req.products() == null) {
            return;
        }
        int order = 0;
        for (ServiceItemRequest.LineItem line : req.products()) {
            if (line == null || line.productId() == null) {
                continue;
            }
            Product product = products.findByIdAndBusinessId(line.productId(), businessId)
                    .orElseThrow(() -> NotFoundException.of("Product", line.productId()));
            ServiceProduct sp = new ServiceProduct();
            sp.setProduct(product);
            sp.setQuantity(line.quantity() == null || line.quantity() < 1 ? 1 : line.quantity());
            sp.setSortOrder(line.sortOrder() == null ? order : line.sortOrder());
            s.addProduct(sp);
            order++;
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
