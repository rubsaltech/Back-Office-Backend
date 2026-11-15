package com.backoffice.pos.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByIdAndStoreId(Long id, Long storeId);

    Page<ProductVariant> findByStoreId(Long storeId, Pageable pageable);

    @Query("select v from ProductVariant v where v.storeId = :storeId and v.onHand <= v.lowStockThreshold")
    Page<ProductVariant> findLowStockByStore(@Param("storeId") Long storeId, Pageable pageable);

    boolean existsByStoreIdAndSku(Long storeId, String sku);

    /**
     * Variant search within a store: matches variant name/sku/barcode, the parent
     * product's name/sku/description, or the category name (case-insensitive).
     */
    @Query("""
            select v from ProductVariant v
              join v.product p
              left join p.category c
            where v.storeId = :storeId and (
                lower(v.variantName) like lower(concat('%', :q, '%'))
                or lower(v.sku) like lower(concat('%', :q, '%'))
                or lower(v.barcode) like lower(concat('%', :q, '%'))
                or lower(p.name) like lower(concat('%', :q, '%'))
                or lower(p.sku) like lower(concat('%', :q, '%'))
                or lower(p.description) like lower(concat('%', :q, '%'))
                or lower(c.name) like lower(concat('%', :q, '%'))
            )""")
    Page<ProductVariant> searchByStore(@Param("storeId") Long storeId, @Param("q") String q, Pageable pageable);

    // --- dashboard aggregates (per store) ---
    @Query("select coalesce(sum(v.quantitySold), 0) from ProductVariant v where v.storeId = :storeId")
    long sumQuantitySoldByStore(@Param("storeId") Long storeId);

    @Query("select count(v) from ProductVariant v where v.storeId = :storeId and v.onHand <= v.lowStockThreshold")
    long countLowStockByStore(@Param("storeId") Long storeId);

    @Query("""
            select v from ProductVariant v join fetch v.product
            where v.storeId = :storeId and v.quantitySold > 0
            order by v.quantitySold desc""")
    List<ProductVariant> findTopSellingByStore(@Param("storeId") Long storeId, Pageable pageable);
}
