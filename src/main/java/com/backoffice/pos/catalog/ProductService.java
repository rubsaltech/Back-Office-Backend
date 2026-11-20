package com.backoffice.pos.catalog;

import com.backoffice.pos.catalog.dto.ImportResult;
import com.backoffice.pos.catalog.dto.ProductRequest;
import com.backoffice.pos.catalog.dto.ProductResponse;
import com.backoffice.pos.catalog.dto.VariantRequest;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.inventory.StockService;
import com.backoffice.pos.label.LabelAttachments;
import com.backoffice.pos.store.StoreResolver;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final LabelAttachments labelAttachments;
    private final StoreResolver storeResolver;
    private final StockService stockService;

    public ProductService(ProductRepository products, ProductVariantRepository variants,
                          CategoryRepository categories, LabelAttachments labelAttachments,
                          StoreResolver storeResolver, StockService stockService) {
        this.products = products;
        this.variants = variants;
        this.categories = categories;
        this.labelAttachments = labelAttachments;
        this.storeResolver = storeResolver;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String query, Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Product> page = StringUtils.hasText(query)
                ? products.searchByStore(storeId, query.trim(), pageable)
                : products.findByStoreId(storeId, pageable);
        return PageResponse.of(page, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(load(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        if (products.existsByStoreIdAndSku(storeId, req.sku())) {
            throw new ConflictException("Product SKU already exists in this store: " + req.sku());
        }
        Product p = new Product();
        p.setBusinessId(businessId);
        p.setStoreId(storeId);
        List<ProductVariant> created = apply(p, req, true);
        products.saveAndFlush(p); // flush so new variants have ids before the ledger write
        // Opening-balance ledger entry for every new variant.
        created.forEach(stockService::recordOpening);
        return ProductResponse.from(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = load(id); // already managed — don't save()/merge (that would copy the graph)
        List<ProductVariant> created = apply(p, req, false);
        // Flush the managed context so cascade-inserts (new variants) and orphan
        // deletes are applied, assigning ids to the actual `created` instances
        // before we write their opening ledger rows.
        variants.flush();
        created.forEach(stockService::recordOpening);
        return ProductResponse.from(p);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(load(id));
    }

    /** One parsed CSV data row (1-based line number + its columns). */
    private record Row(int num, String[] cols) {
    }

    /**
     * CSV import. Rows are grouped by product SKU (column 1), so a product with
     * several variants is expressed as several rows sharing that SKU. Columns:
     * <pre>
     *   0 name, 1 sku, 2 barcode, 3 category, 4 price, 5 taxAmount,
     *   6 availableQty, 7 status,
     *   8 variantSku, 9 variantName, 10 attributes ("Color=Black;Storage=128GB"),
     *   11 purchasedPrice, 12 lowStockThreshold, 13 trackingType (NONE|SERIAL)
     * </pre>
     * Columns 8+ are optional; a group whose rows carry a variantSku becomes a
     * multi-variant product, otherwise a single-row group is a simple product
     * with one Default variant (backward compatible with the old 8-column file).
     */
    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        Map<String, Category> byName = categories.findByStoreIdOrderByNameAsc(storeId).stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), Function.identity(), (a, b) -> a));

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        // ---- Parse and group rows by product SKU (preserving file order) ----
        Map<String, List<Row>> groups = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                String[] c = line.split(",", -1);
                String sku = value(c, 1);
                if (!StringUtils.hasText(sku)) {
                    errors.add("Row " + row + ": missing SKU");
                    skipped++;
                    continue;
                }
                groups.computeIfAbsent(sku, k -> new ArrayList<>()).add(new Row(row, c));
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read CSV file: " + e.getMessage());
        }

        // ---- Build one product (with its variants) per group ----
        for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {
            String sku = entry.getKey();
            List<Row> rows = entry.getValue();
            try {
                if (products.existsByStoreIdAndSku(storeId, sku)) {
                    skipped++;
                    continue;
                }
                boolean variantMode = rows.stream().anyMatch(r -> StringUtils.hasText(value(r.cols(), 8)));
                if (!variantMode && rows.size() > 1) {
                    errors.add("SKU " + sku + ": multiple rows need a variantSku column to be imported as variants");
                    skipped++;
                    continue;
                }

                String[] head = rows.get(0).cols();
                Product p = new Product();
                p.setBusinessId(businessId);
                p.setStoreId(storeId);
                p.setName(value(head, 0));
                p.setSku(sku);
                p.setBarcode(value(head, 2));
                p.setCategory(byName.get(value(head, 3).toLowerCase()));
                p.setTaxAmount(decimal(value(head, 5)));
                p.setStatus(statusOf(value(head, 7)));
                p.setHasVariants(variantMode);

                List<ProductVariant> created = new ArrayList<>();
                String groupError = null;
                Set<String> variantSkus = new java.util.HashSet<>();
                for (int i = 0; i < rows.size(); i++) {
                    String[] c = rows.get(i).cols();
                    String vSku = variantMode ? value(c, 8) : sku;
                    if (variantMode && !StringUtils.hasText(vSku)) {
                        groupError = "SKU " + sku + ": every variant row needs a variantSku";
                        break;
                    }
                    if (!variantSkus.add(vSku) || variants.existsByStoreIdAndSku(storeId, vSku)) {
                        groupError = "Variant SKU already exists: " + vSku;
                        break;
                    }
                    ProductVariant v = new ProductVariant();
                    v.setBusinessId(businessId);
                    v.setStoreId(storeId);
                    v.setDefault(i == 0);
                    v.setVariantName(variantMode && StringUtils.hasText(value(c, 9)) ? value(c, 9) : "Default");
                    v.setAttributes(variantMode ? parseAttributes(value(c, 10)) : new LinkedHashMap<>());
                    v.setSku(vSku);
                    v.setBarcode(value(c, 2));
                    v.setPrice(decimal(value(c, 4)));
                    v.setPurchasedPrice(decimal(value(c, 11)));
                    TrackingType tracking = "SERIAL".equalsIgnoreCase(value(c, 13)) ? TrackingType.SERIAL : TrackingType.NONE;
                    v.setTrackingType(tracking);
                    // Serialized variants open at zero — units are added later by IMEI.
                    int qty = tracking == TrackingType.SERIAL ? 0 : integer(value(c, 6));
                    v.setOnHand(qty);
                    v.setTotalReceived(qty);
                    if (StringUtils.hasText(value(c, 12))) {
                        v.setLowStockThreshold(integer(value(c, 12)));
                    }
                    v.setStatus(p.getStatus());
                    p.addVariant(v);
                    created.add(v);
                }
                if (groupError != null) {
                    errors.add(groupError);
                    skipped++;
                    continue;
                }

                products.save(p);
                created.forEach(stockService::recordOpening);
                imported++;
            } catch (Exception ex) {
                errors.add("SKU " + sku + ": " + ex.getMessage());
                skipped++;
            }
        }
        return new ImportResult(imported, skipped, errors);
    }

    // ---- helpers ----

    private static CatalogStatus statusOf(String s) {
        return "INACTIVE".equalsIgnoreCase(s) ? CatalogStatus.INACTIVE : CatalogStatus.ACTIVE;
    }

    /** Parse "Color=Black;Storage=128GB" into an ordered attribute map. */
    private static Map<String, String> parseAttributes(String raw) {
        Map<String, String> attrs = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) {
            return attrs;
        }
        for (String pair : raw.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String k = pair.substring(0, eq).trim();
                String val = pair.substring(eq + 1).trim();
                if (!k.isEmpty() && !val.isEmpty()) {
                    attrs.put(k, val);
                }
            }
        }
        return attrs;
    }

    private Product load(Long id) {
        return products.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Product", id));
    }

    /**
     * Writes the request onto the product and reconciles its variants. Returns the
     * list of newly-created variants (so the caller can log their opening balance
     * after persistence assigns ids). Stock counters of existing variants are left
     * untouched here — only the inventory endpoints move stock.
     */
    private List<ProductVariant> apply(Product p, ProductRequest req, boolean isCreate) {
        Long businessId = TenantContext.requireBusinessId();
        p.setName(req.name());
        p.setSku(req.sku());
        p.setBarcode(req.barcode());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setCategory(req.categoryId() == null ? null
                : categories.findByIdAndStoreId(req.categoryId(), p.getStoreId())
                .orElseThrow(() -> NotFoundException.of("Category", req.categoryId())));
        p.setTaxAmount(nvl(req.taxAmount()));
        p.setDiscountTitle(req.discountTitle());
        p.setDiscountAmount(nvl(req.discountAmount()));
        if (req.status() != null) {
            p.setStatus(req.status());
        }
        p.setHasVariants(Boolean.TRUE.equals(req.hasVariants()));
        p.setSaleCaptureLabels(req.saleCaptureLabels() == null ? new ArrayList<>() : new ArrayList<>(req.saleCaptureLabels()));

        List<ProductVariant> created = reconcileVariants(p, req, isCreate);
        rebuildLabels(p, req, businessId);
        return created;
    }

    private List<ProductVariant> reconcileVariants(Product p, ProductRequest req, boolean isCreate) {
        List<VariantRequest> reqVariants = req.variants() == null ? List.of() : req.variants();
        if (reqVariants.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A product must have at least one variant");
        }
        // Exactly one default; if the client flagged none, the first wins.
        boolean anyDefault = reqVariants.stream().anyMatch(vr -> Boolean.TRUE.equals(vr.isDefault()));

        Map<Long, ProductVariant> existing = new LinkedHashMap<>();
        for (ProductVariant v : p.getVariants()) {
            if (v.getId() != null) {
                existing.put(v.getId(), v);
            }
        }
        // Drop only the variants the client removed (kept ones stay in place so
        // their id — and their ledger/units/order links — survive). A clear()+
        // re-add would re-key the whole collection and orphan the stock ledger.
        Set<Long> keptIds = new HashSet<>();
        for (VariantRequest vr : reqVariants) {
            if (vr.id() != null) {
                keptIds.add(vr.id());
            }
        }
        p.getVariants().removeIf(v -> v.getId() != null && !keptIds.contains(v.getId()));

        List<ProductVariant> created = new ArrayList<>();
        for (int i = 0; i < reqVariants.size(); i++) {
            VariantRequest vr = reqVariants.get(i);
            boolean isDefault = Boolean.TRUE.equals(vr.isDefault()) || (!anyDefault && i == 0);

            ProductVariant v = (vr.id() != null) ? existing.get(vr.id()) : null;
            boolean isNew = (v == null);
            if (isNew) {
                v = new ProductVariant();
                v.setBusinessId(p.getBusinessId());
                v.setStoreId(p.getStoreId());
                // Serialized variants open at zero — their stock is added later by
                // receiving units with IMEIs, so on-hand always equals unit count.
                boolean serial = vr.trackingType() == TrackingType.SERIAL;
                int opening = serial || vr.openingQty() == null || vr.openingQty() < 0 ? 0 : vr.openingQty();
                v.setOnHand(opening);
                v.setTotalReceived(opening);
                p.addVariant(v);
                created.add(v);
            }
            // Catalog fields are always writable; stock counters are not touched on edit.
            v.setVariantName(StringUtils.hasText(vr.variantName()) ? vr.variantName() : "Default");
            v.setAttributes(vr.attributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(vr.attributes()));
            v.setDefault(isDefault);
            v.setSku(vr.sku());
            v.setBarcode(vr.barcode());
            v.setPrice(nvl(vr.price()));
            v.setPurchasedPrice(nvl(vr.purchasedPrice()));
            v.setTaxAmount(nvl(vr.taxAmount()));
            v.setDiscountTitle(vr.discountTitle());
            v.setDiscountAmount(nvl(vr.discountAmount()));
            v.setTrackingType(vr.trackingType() == null ? TrackingType.NONE : vr.trackingType());
            if (vr.lowStockThreshold() != null) {
                v.setLowStockThreshold(vr.lowStockThreshold());
            }
            if (vr.status() != null) {
                v.setStatus(vr.status());
            }
        }
        return created;
    }

    private void rebuildLabels(Product p, ProductRequest req, Long businessId) {
        p.clearLabels();
        for (LabelAttachments.Resolved r : labelAttachments.resolve(req.labels(), businessId)) {
            ProductLabel pl = new ProductLabel();
            pl.setLabelId(r.labelId());
            pl.setLabelName(r.name());
            pl.setLabelType(r.type());
            pl.setSortOrder(r.sortOrder());
            pl.getValues().addAll(r.values());
            p.addLabel(pl);
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
