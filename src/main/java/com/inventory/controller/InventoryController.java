package com.inventory.controller;

import com.inventory.dto.ProductResponse;
import com.inventory.dto.StockMovementRequest;
import com.inventory.entity.InventoryTransaction;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock movement, transaction history, low-stock alerts")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;

    @PostMapping("/{productId}/stock-in")
    @Operation(summary = "Increase stock for a product and log an IN transaction")
    public ResponseEntity<Map<String, String>> stockIn(
            @PathVariable Long productId, @Valid @RequestBody StockMovementRequest request) {
        inventoryService.stockIn(productId, request);
        return ResponseEntity.ok(Map.of("status", "stock-in recorded"));
    }

    @PostMapping("/{productId}/stock-out")
    @Operation(summary = "Decrease stock for a product; rejects if insufficient quantity (409)")
    public ResponseEntity<Map<String, String>> stockOut(
            @PathVariable Long productId, @Valid @RequestBody StockMovementRequest request) {
        inventoryService.stockOut(productId, request);
        return ResponseEntity.ok(Map.of("status", "stock-out recorded"));
    }

    @GetMapping("/{productId}/transactions")
    @Operation(summary = "Paginated stock-movement history for one product")
    public Page<InventoryTransaction> transactionHistory(
            @PathVariable Long productId, Pageable pageable) {
        return inventoryService.getTransactionHistory(productId, pageable);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Products at or below their reorder level")
    public List<ProductResponse> lowStock() {
        return productService.getLowStockProducts();
    }
}
