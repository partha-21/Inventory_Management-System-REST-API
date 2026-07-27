package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Single filtered + paginated query.
     * @EntityGraph eagerly loads category/supplier/inventory in the SAME query
     * (via LEFT JOIN) instead of Hibernate firing one extra SELECT per row for
     * each lazy association — this is the fix for the classic N+1 problem.
     * Safe to paginate here because all three associations are *-to-one
     * (no collection fetch join), so there's no row-multiplication issue.
     */
    @EntityGraph(attributePaths = {"category", "supplier", "inventory"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
              AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<Product> search(
            @Param("categoryId") Long categoryId,
            @Param("supplierId") Long supplierId,
            @Param("name") String name,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "supplier", "inventory"})
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.active = true")
    Product findActiveById(@Param("id") Long id);

    /** Products at or below their reorder threshold — one query, no N+1. */
    @Query("""
            SELECT p FROM Product p
            JOIN p.inventory i
            WHERE p.active = true AND i.quantityOnHand <= p.reorderLevel
            """)
    List<Product> findLowStockProducts();
}
