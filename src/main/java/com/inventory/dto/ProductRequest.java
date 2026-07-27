package com.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Never bind incoming JSON directly to the Product entity — always go
 * through a validated request DTO like this one.
 */
@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50)
    private String sku;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "supplierId is required")
    private Long supplierId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice cannot be negative")
    private BigDecimal unitPrice;

    @Min(value = 0, message = "reorderLevel cannot be negative")
    private Integer reorderLevel = 10;

    /** Optional initial stock quantity when creating a brand-new product. */
    @Min(0)
    private Integer initialQuantity = 0;

    private String warehouseLocation;
}
