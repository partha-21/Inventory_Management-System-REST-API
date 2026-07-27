package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockMovementRequest {

    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private String referenceNote;

    private String createdBy;
}
