package br.com.tiakarol.api.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record VaccineLotResponse(UUID id, String vaccineName, String vaccineType, String lotCode, LocalDate expirationDate,
                          String manufacturer, String supplier, String invoiceNumber, BigDecimal purchasePrice,
                          BigDecimal salePrice, String notes, boolean active, int physicalQuantity,
                          int reservedQuantity, int availableQuantity) {
}
