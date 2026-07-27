package com.inventory.controller;

import com.inventory.repository.ReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregate reporting queries computed in the database")
public class ReportController {

    private final ReportRepository reportRepository;

    @GetMapping("/stock-value")
    @Operation(summary = "Total current inventory value (SUM(qty * unit_price) in one query)")
    public Map<String, Double> stockValue() {
        return Map.of("totalStockValue", reportRepository.getTotalStockValue());
    }

    @GetMapping("/category-summary")
    @Operation(summary = "Product count, units, and value grouped by category")
    public List<Map<String, Object>> categorySummary() {
        return reportRepository.getCategorySummary();
    }

    @GetMapping("/top-moving-products")
    @Operation(summary = "Most-transacted products in the last N days (default 30)")
    public List<Map<String, Object>> topMovingProducts(
            @RequestParam(defaultValue = "30") int days) {
        return reportRepository.getTopMovingProducts(days);
    }
}
