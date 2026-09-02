package br.com.tiakarol.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialProjectionResponse(LocalDate fromDate, LocalDate toDate,
                                          BigDecimal receivables, BigDecimal recurringExpenses,
                                          List<Item> items) {
    public record Item(String source, UUID sourceId, String description, LocalDate expectedOn,
                       BigDecimal amount) { }
}
