package com.backoffice.pos.catalog;

import com.backoffice.pos.catalog.dto.CategoryRequest;
import com.backoffice.pos.catalog.dto.CategoryResponse;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final StoreResolver storeResolver;

    public CategoryService(CategoryRepository categories, ProductRepository products, StoreResolver storeResolver) {
        this.categories = categories;
        this.products = products;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> list(String query, Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Category> page = StringUtils.hasText(query)
                ? categories.findByStoreIdAndNameContainingIgnoreCase(storeId, query, pageable)
                : categories.findByStoreId(storeId, pageable);
        return PageResponse.of(page, c -> CategoryResponse.from(c, products.countByCategory_Id(c.getId())));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> all() {
        return categories.findByStoreIdOrderByNameAsc(storeResolver.currentStoreId())
                .stream().map(c -> CategoryResponse.from(c, products.countByCategory_Id(c.getId()))).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        if (categories.existsByStoreIdAndName(storeId, req.name())) {
            throw new ConflictException("Category already exists in this store: " + req.name());
        }
        Category c = new Category();
        c.setBusinessId(businessId);
        c.setStoreId(storeId);
        apply(c, req);
        return CategoryResponse.from(categories.save(c), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = load(id);
        apply(c, req);
        return CategoryResponse.from(categories.save(c), products.countByCategory_Id(c.getId()));
    }

    @Transactional
    public void delete(Long id) {
        categories.delete(load(id));
    }

    private Category load(Long id) {
        return categories.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Category", id));
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.name());
        c.setImageUrl(req.imageUrl());
        if (req.status() != null) {
            c.setStatus(req.status());
        }
    }
}
