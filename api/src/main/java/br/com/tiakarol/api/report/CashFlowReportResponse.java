package br.com.tiakarol.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CashFlowReportResponse(LocalDate fromDate, LocalDate toDate,
                                     BigDecimal income, BigDecimal expenses, BigDecimal balance,
                                     List<Item> entries) {
    public record Item(String source, UUID sourceId, String type, String description,
                       String paymentMethod, BigDecimal amount, LocalDate occurredOn) { }
}
