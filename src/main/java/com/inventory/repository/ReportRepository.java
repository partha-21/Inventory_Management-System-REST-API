package com.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.inventory.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * These reports are the "optimized SQL queries" part of the project:
 * every one of them is a single aggregate query computed by MySQL,
 * not a loop over all products in Java. That distinction is exactly
 * what an interviewer is checking for when they ask "did you actually
 * optimize anything, or just say that."
 */
@Repository
public interface ReportRepository extends JpaRepository<Product, Long> {

    /** Total value currently sitting in inventory: SUM(qty * unit_price). */
    @Query(value = """
            SELECT COALESCE(SUM(i.quantity_on_hand * p.unit_price), 0)
            FROM product p
            JOIN inventory i ON i.product_id = p.id
            WHERE p.is_active = true
            """, nativeQuery = true)
    Double getTotalStockValue();

    /** Stock count + value grouped by category, in one query. */
    @Query(value = """
            SELECT c.name AS categoryName,
                   COUNT(p.id) AS productCount,
                   COALESCE(SUM(i.quantity_on_hand), 0) AS totalUnits,
                   COALESCE(SUM(i.quantity_on_hand * p.unit_price), 0) AS totalValue
            FROM category c
            LEFT JOIN product p ON p.category_id = c.id AND p.is_active = true
            LEFT JOIN inventory i ON i.product_id = p.id
            GROUP BY c.id, c.name
            ORDER BY totalValue DESC
            """, nativeQuery = true)
    List<Map<String, Object>> getCategorySummary();

    /** Most-transacted products in the last N days, ranked by total quantity moved. */
    @Query(value = """
            SELECT p.id AS productId,
                   p.name AS productName,
                   p.sku AS sku,
                   SUM(t.quantity) AS totalQuantityMoved,
                   COUNT(t.id) AS transactionCount
            FROM inventory_transaction t
            JOIN product p ON p.id = t.product_id
            WHERE t.created_at >= (CURRENT_DATE - INTERVAL :days DAY)
            GROUP BY p.id, p.name, p.sku
            ORDER BY totalQuantityMoved DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Map<String, Object>> getTopMovingProducts(@Param("days") int days);
}
