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

    long countByBusinessId(Long businessId);

    long countByBusinessIdAndStatus(Long businessId, CatalogStatus status);

    long countByCategory_Id(Long categoryId);

    @Query("select coalesce(sum(p.quantitySold), 0) from Product p where p.businessId = :businessId")
    long sumQuantitySold(@Param("businessId") Long businessId);

    List<Product> findTop7ByBusinessIdOrderByQuantitySoldDesc(Long businessId);
}
