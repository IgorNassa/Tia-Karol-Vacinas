package br.com.tiakarol.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialSummaryResponse(LocalDate fromDate, LocalDate toDate,
                                       BigDecimal appointmentIncome, BigDecimal otherIncome,
                                       BigDecimal expenses, BigDecimal realizedBalance,
                                       BigDecimal receivablesProjection, BigDecimal recurringExpensesProjection,
                                       BigDecimal projectedBalance, List<MethodTotal> incomeByMethod) {
    public record MethodTotal(String method, BigDecimal amount) { }
}
