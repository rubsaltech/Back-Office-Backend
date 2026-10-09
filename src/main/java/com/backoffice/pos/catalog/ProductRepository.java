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

    /** Generic product search within a store: matches name, SKU or barcode (case-insensitive). */
    @Query("""
            select p from Product p
            where p.storeId = :storeId and (
                lower(p.name) like lower(concat('%', :q, '%'))
                or lower(p.sku) like lower(concat('%', :q, '%'))
                or lower(p.barcode) like lower(concat('%', :q, '%'))
            )""")
    Page<Product> searchByStore(@Param("storeId") Long storeId, @Param("q") String q, Pageable pageable);

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);

    boolean existsByStoreIdAndSku(Long storeId, String sku);

    long countByBusinessId(Long businessId);

    long countByBusinessIdAndStatus(Long businessId, CatalogStatus status);

    long countByCategory_Id(Long categoryId);

    @Query("select coalesce(sum(p.quantitySold), 0) from Product p where p.businessId = :businessId")
    long sumQuantitySold(@Param("businessId") Long businessId);

    List<Product> findTop7ByBusinessIdOrderByQuantitySoldDesc(Long businessId);

    // --- store-scoped dashboard aggregates ---
    long countByStoreId(Long storeId);

    long countByStoreIdAndStatus(Long storeId, CatalogStatus status);

    @Query("select coalesce(sum(p.quantitySold), 0) from Product p where p.storeId = :storeId")
    long sumQuantitySoldByStore(@Param("storeId") Long storeId);

    List<Product> findTop7ByStoreIdOrderByQuantitySoldDesc(Long storeId);
}
