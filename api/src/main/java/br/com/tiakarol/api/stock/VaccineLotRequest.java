package br.com.tiakarol.api.stock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

record VaccineLotRequest(
        @NotBlank String vaccineName,
        String vaccineType,
        @NotBlank String lotCode,
        @NotNull @Future LocalDate expirationDate,
        String manufacturer,
        String supplier,
        String invoiceNumber,
        @NotNull @DecimalMin("0.00") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.00") BigDecimal salePrice,
        String notes,
        @Min(1) int initialQuantity) {
}
