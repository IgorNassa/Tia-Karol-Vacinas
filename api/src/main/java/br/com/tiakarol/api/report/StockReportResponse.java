package br.com.tiakarol.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StockReportResponse(int physicalQuantity, int reservedQuantity, int availableQuantity,
                                  BigDecimal acquisitionValue, BigDecimal potentialSaleValue,
                                  List<Item> lots) {
    public record Item(UUID lotId, UUID vaccineId, String vaccineName, String lotCode,
                       LocalDate expirationDate, boolean active, int physicalQuantity,
                       int reservedQuantity, int availableQuantity, BigDecimal acquisitionValue,
                       BigDecimal potentialSaleValue) { }
}
