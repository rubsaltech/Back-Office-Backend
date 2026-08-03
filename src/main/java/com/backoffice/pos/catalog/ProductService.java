package com.backoffice.pos.catalog;

import com.backoffice.pos.catalog.dto.ImportResult;
import com.backoffice.pos.catalog.dto.ProductRequest;
import com.backoffice.pos.catalog.dto.ProductResponse;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Product> page = StringUtils.hasText(query)
                ? products.findByBusinessIdAndNameContainingIgnoreCase(businessId, query, pageable)
                : products.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(load(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (products.existsByBusinessIdAndSku(businessId, req.sku())) {
            throw new ConflictException("Product SKU already exists: " + req.sku());
        }
        Product p = new Product();
        p.setBusinessId(businessId);
        apply(p, req);
        return ProductResponse.from(products.save(p));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = load(id);
        apply(p, req);
        return ProductResponse.from(products.save(p));
    }

    @Transactional
    public void delete(Long id) {
        products.delete(load(id));
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        Long businessId = TenantContext.requireBusinessId();
        Map<String, Category> byName = categories.findByBusinessIdOrderByNameAsc(businessId).stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), Function.identity(), (a, b) -> a));

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header: name,sku,barcode,category,price,taxAmount,availableQty,status
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                String[] c = line.split(",", -1);
                try {
                    String sku = value(c, 1);
                    if (!StringUtils.hasText(sku)) {
                        errors.add("Row " + row + ": missing SKU");
                        skipped++;
                        continue;
                    }
                    if (products.existsByBusinessIdAndSku(businessId, sku)) {
                        skipped++;
                        continue;
                    }
                    Product p = new Product();
                    p.setBusinessId(businessId);
                    p.setName(value(c, 0));
                    p.setSku(sku);
                    p.setBarcode(value(c, 2));
                    p.setCategory(byName.get(value(c, 3).toLowerCase()));
                    p.setPrice(decimal(value(c, 4)));
                    p.setTaxAmount(decimal(value(c, 5)));
                    p.setAvailableQty(integer(value(c, 6)));
                    p.setStatus("INACTIVE".equalsIgnoreCase(value(c, 7)) ? CatalogStatus.INACTIVE : CatalogStatus.ACTIVE);
                    products.save(p);
                    imported++;
                } catch (Exception ex) {
                    errors.add("Row " + row + ": " + ex.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read CSV file: " + e.getMessage());
        }
        return new ImportResult(imported, skipped, errors);
    }

    // ---- helpers ----

    private Product load(Long id) {
        return products.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Product", id));
    }

    private void apply(Product p, ProductRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        p.setName(req.name());
        p.setSku(req.sku());
        p.setBarcode(req.barcode());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setCategory(req.categoryId() == null ? null
                : categories.findByIdAndBusinessId(req.categoryId(), businessId)
                .orElseThrow(() -> NotFoundException.of("Category", req.categoryId())));
        p.setPrice(nvl(req.price()));
        p.setTaxAmount(nvl(req.taxAmount()));
        p.setDiscountTitle(req.discountTitle());
        p.setDiscountAmount(nvl(req.discountAmount()));
        if (req.status() != null) {
            p.setStatus(req.status());
        }
        if (req.availableQty() != null) {
            p.setAvailableQty(req.availableQty());
        }
        if (req.totalQty() != null) {
            p.setTotalQty(req.totalQty());
        }
        rebuildModifiers(p, req);
    }

    private void rebuildModifiers(Product p, ProductRequest req) {
        p.clearModifierGroups();
        if (req.modifierGroups() == null) {
            return;
        }
        for (ProductRequest.Group g : req.modifierGroups()) {
            ModifierGroup group = new ModifierGroup();
            group.setName(g.name());
            group.setRequired(g.required());
            group.setMinSelect(g.minSelect());
            group.setMaxSelect(g.maxSelect());
            group.setSortOrder(g.sortOrder());
            if (g.options() != null) {
                for (ProductRequest.Option o : g.options()) {
                    ModifierOption option = new ModifierOption();
                    option.setName(o.name());
                    option.setPriceDelta(nvl(o.priceDelta()));
                    option.setDefault(o.isDefault());
                    option.setSortOrder(o.sortOrder());
                    group.addOption(option);
                }
            }
            p.addModifierGroup(group);
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String value(String[] arr, int i) {
        return i < arr.length ? arr[i].trim() : "";
    }

    private static BigDecimal decimal(String s) {
        return StringUtils.hasText(s) ? new BigDecimal(s) : BigDecimal.ZERO;
    }

    private static int integer(String s) {
        return StringUtils.hasText(s) ? Integer.parseInt(s) : 0;
    }
}
