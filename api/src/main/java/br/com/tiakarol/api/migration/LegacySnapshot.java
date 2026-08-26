package br.com.tiakarol.api.migration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record LegacySnapshot(
        List<PatientRow> patients,
        List<VaccineRow> vaccines,
        List<ApplicationRow> applications,
        List<OtherEntryRow> otherEntries,
        List<RecurringExpenseRow> recurringExpenses,
        List<ExpensePaymentRow> expensePayments,
        long configurationCount
) {
    record PatientRow(long id, String name, String document, String birthDate, String phone,
                      String allergies, String guardianName, String guardianDocument,
                      String secondGuardianName, String secondGuardianDocument, long photoBytes) {
    }

    record VaccineRow(long id, String name, String type, String lotCode, String expirationDate,
                      String manufacturer, String supplier, String invoiceNumber, Integer totalQuantity,
                      Integer availableQuantity, BigDecimal purchasePrice, BigDecimal salePrice) {
    }

    record ApplicationRow(long id, long patientId, long vaccineId, LocalDateTime occurredAt, String status,
                          String paymentMethod, BigDecimal amount, BigDecimal grossAmount, BigDecimal discount) {
    }

    record OtherEntryRow(long id, String name, String type, BigDecimal amount, String occurredOn) {
    }

    record RecurringExpenseRow(long id, String name, BigDecimal defaultAmount, Integer variableAmount,
                               Integer dueDay, BigDecimal lastPaidAmount, String lastPaidOn) {
    }

    record ExpensePaymentRow(long id, long expenseId, BigDecimal amount, String paidOn) {
    }
}
