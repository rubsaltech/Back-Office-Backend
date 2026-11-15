package com.backoffice.pos.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByBusinessId(Long businessId, Pageable pageable);

    Page<Product> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name, Pageable pageable);

    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndSku(Long businessId, String sku);

    // --- store-scoped (catalog is per store) ---
    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    Page<Product> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name, Pageable pageable);

    /**
     * Generic product search within a store: matches name, SKU, barcode,
     * description or category name (case-insensitive). LEFT JOIN so products with
     * no category are still searchable by their other fields.
     */
    @Query("""
            select p from Product p left join p.category c
            where p.storeId = :storeId and (
                lower(p.name) like lower(concat('%', :q, '%'))
                or lower(p.sku) like lower(concat('%', :q, '%'))
                or lower(p.barcode) like lower(concat('%', :q, '%'))
                or lower(p.description) like lower(concat('%', :q, '%'))
                or lower(c.name) like lower(concat('%', :q, '%'))
            )""")
    Page<Product> searchByStore(@Param("storeId") Long storeId, @Param("q") String q, Pageable pageable);

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);

    boolean existsByStoreIdAndSku(Long storeId, String sku);

    long countByBusinessId(Long businessId);

    long countByBusinessIdAndStatus(Long businessId, CatalogStatus status);

    long countByCategory_Id(Long categoryId);

    // --- store-scoped dashboard aggregates ---
    long countByStoreId(Long storeId);

    long countByStoreIdAndStatus(Long storeId, CatalogStatus status);
}
