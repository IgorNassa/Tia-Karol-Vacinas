package br.com.tiakarol.api.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record StockMovementRequest(
        @NotNull StockMovementType type,
        @Min(1) int quantity,
        String reason) {
}
