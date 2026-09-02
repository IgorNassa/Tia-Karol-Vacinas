package br.com.tiakarol.api.migration;

import static br.com.tiakarol.api.migration.LegacyMigrationIssue.Severity.ERROR;
import static br.com.tiakarol.api.migration.LegacyMigrationIssue.Severity.WARNING;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class LegacyMigrationAnalyzer {
    LegacyMigrationReport analyze(LegacySnapshot snapshot) {
        List<LegacyMigrationIssue> issues = new ArrayList<>();
        analyzePatients(snapshot.patients(), issues);
        analyzeVaccines(snapshot.vaccines(), issues);
        analyzeApplications(snapshot, issues);
        analyzeFinancial(snapshot, issues);

        long errors = issues.stream().filter(issue -> issue.severity() == ERROR).count();
        long warnings = issues.size() - errors;
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("patients", (long) snapshot.patients().size());
        counts.put("vaccines", (long) snapshot.vaccines().size());
        counts.put("applications", (long) snapshot.applications().size());
        counts.put("otherFinancialEntries", (long) snapshot.otherEntries().size());
        counts.put("recurringExpenses", (long) snapshot.recurringExpenses().size());
        counts.put("expensePayments", (long) snapshot.expensePayments().size());
        counts.put("clinicConfigurations", snapshot.configurationCount());
        return new LegacyMigrationReport(UUID.randomUUID(), OffsetDateTime.now(), "TIA_KAROL_SWING",
                "DRY_RUN", errors == 0, errors, warnings, counts, List.copyOf(issues));
    }

    private void analyzePatients(List<LegacySnapshot.PatientRow> patients, List<LegacyMigrationIssue> issues) {
        Map<String, List<Long>> documentOwners = new HashMap<>();
        for (LegacySnapshot.PatientRow patient : patients) {
            String id = Long.toString(patient.id());
            if (blank(patient.name())) {
                add(issues, ERROR, "PATIENT_NAME_MISSING", "PATIENT", id, "Nome obrigatório ausente.");
            }
            String document = LegacyValueNormalizer.digits(patient.document());
            if (document.isBlank()) {
                add(issues, WARNING, "PATIENT_IDENTITY_REVIEW", "PATIENT", id,
                        "Documento ausente; classificar como estrangeiro ou recém-nascido.");
            } else {
                documentOwners.computeIfAbsent(document, ignored -> new ArrayList<>()).add(patient.id());
                if (document.length() == 11 && !LegacyValueNormalizer.validCpf(document)) {
                    add(issues, ERROR, "PATIENT_CPF_INVALID", "PATIENT", id, "CPF inválido.");
                } else if (document.length() != 11) {
                    add(issues, WARNING, "PATIENT_IDENTITY_REVIEW", "PATIENT", id,
                            "Documento não possui formato de CPF; confirmar tipo de identificação.");
                }
            }
            invalidDate(patient.birthDate(), issues, "PATIENT_BIRTH_DATE_INVALID", "PATIENT", id);
            if (patient.photoBytes() > 0) {
                add(issues, WARNING, "PATIENT_PHOTO_REQUIRES_DESTINATION", "PATIENT", id,
                        "Foto encontrada e ainda sem destino definido no modelo novo.");
            }
        }
        documentOwners.values().stream().filter(ids -> ids.size() > 1).forEach(ids -> ids.forEach(id ->
                add(issues, ERROR, "PATIENT_DOCUMENT_DUPLICATED", "PATIENT", id.toString(),
                        "Documento repetido em " + ids.size() + " pacientes.")));
    }

    private void analyzeVaccines(List<LegacySnapshot.VaccineRow> vaccines, List<LegacyMigrationIssue> issues) {
        Set<String> lotKeys = new HashSet<>();
        for (LegacySnapshot.VaccineRow vaccine : vaccines) {
            String id = Long.toString(vaccine.id());
            if (blank(vaccine.name())) add(issues, ERROR, "VACCINE_NAME_MISSING", "VACCINE", id, "Nome ausente.");
            if (blank(vaccine.lotCode())) add(issues, ERROR, "VACCINE_LOT_MISSING", "VACCINE", id, "Lote ausente.");
            invalidDate(vaccine.expirationDate(), issues, "VACCINE_EXPIRATION_INVALID", "VACCINE", id);
            String lotKey = LegacyValueNormalizer.canonicalText(vaccine.name()) + "|"
                    + LegacyValueNormalizer.canonicalText(vaccine.lotCode());
            if (!lotKeys.add(lotKey)) {
                add(issues, WARNING, "VACCINE_LOT_DUPLICATED", "VACCINE", id,
                        "Nome e lote aparecem mais de uma vez; os dados precisam ser comparados.");
            }
            if (negative(vaccine.totalQuantity()) || negative(vaccine.availableQuantity())) {
                add(issues, ERROR, "VACCINE_STOCK_NEGATIVE", "VACCINE", id, "Quantidade negativa.");
            }
            if (vaccine.totalQuantity() != null && vaccine.availableQuantity() != null
                    && vaccine.availableQuantity() > vaccine.totalQuantity()) {
                add(issues, WARNING, "VACCINE_AVAILABLE_EXCEEDS_TOTAL", "VACCINE", id,
                        "Quantidade disponível maior que a quantidade total histórica.");
            }
            if (negative(vaccine.purchasePrice()) || negative(vaccine.salePrice())) {
                add(issues, ERROR, "VACCINE_PRICE_NEGATIVE", "VACCINE", id, "Preço negativo.");
            }
        }
    }

    private void analyzeApplications(LegacySnapshot snapshot, List<LegacyMigrationIssue> issues) {
        Set<Long> patientIds = snapshot.patients().stream().map(LegacySnapshot.PatientRow::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> vaccineIds = snapshot.vaccines().stream().map(LegacySnapshot.VaccineRow::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> knownStatuses = Set.of("AGENDADO", "CONFIRMADO", "APLICADO", "CANCELADO", "FALTOU");
        for (LegacySnapshot.ApplicationRow application : snapshot.applications()) {
            String id = Long.toString(application.id());
            if (!patientIds.contains(application.patientId())) {
                add(issues, ERROR, "APPLICATION_PATIENT_ORPHAN", "APPLICATION", id, "Paciente referenciado não existe.");
            }
            if (!vaccineIds.contains(application.vaccineId())) {
                add(issues, ERROR, "APPLICATION_VACCINE_ORPHAN", "APPLICATION", id, "Vacina referenciada não existe.");
            }
            if (application.occurredAt() == null) {
                add(issues, ERROR, "APPLICATION_DATE_MISSING", "APPLICATION", id, "Data e hora ausentes.");
            }
            if (!knownStatuses.contains(LegacyValueNormalizer.canonicalText(application.status()))) {
                add(issues, WARNING, "APPLICATION_STATUS_UNKNOWN", "APPLICATION", id,
                        "Status precisa de mapeamento manual.");
            }
            if (negative(application.amount()) || negative(application.grossAmount()) || negative(application.discount())) {
                add(issues, ERROR, "APPLICATION_AMOUNT_NEGATIVE", "APPLICATION", id, "Valor financeiro negativo.");
            }
        }
    }

    private void analyzeFinancial(LegacySnapshot snapshot, List<LegacyMigrationIssue> issues) {
        for (LegacySnapshot.OtherEntryRow entry : snapshot.otherEntries()) {
            String id = Long.toString(entry.id());
            if (blank(entry.name())) add(issues, ERROR, "FINANCIAL_DESCRIPTION_MISSING", "OTHER_ENTRY", id, "Descrição ausente.");
            if (entry.amount() == null || negative(entry.amount())) {
                add(issues, ERROR, "FINANCIAL_AMOUNT_INVALID", "OTHER_ENTRY", id, "Valor ausente ou negativo.");
            }
            invalidDate(entry.occurredOn(), issues, "FINANCIAL_DATE_INVALID", "OTHER_ENTRY", id);
        }
        Set<Long> expenseIds = snapshot.recurringExpenses().stream().map(LegacySnapshot.RecurringExpenseRow::id)
                .collect(java.util.stream.Collectors.toSet());
        for (LegacySnapshot.RecurringExpenseRow expense : snapshot.recurringExpenses()) {
            String id = Long.toString(expense.id());
            if (expense.dueDay() == null || expense.dueDay() < 1 || expense.dueDay() > 31) {
                add(issues, ERROR, "RECURRING_DUE_DAY_INVALID", "RECURRING_EXPENSE", id, "Dia de vencimento fora de 1 a 31.");
            }
            if (expense.defaultAmount() == null || negative(expense.defaultAmount())) {
                add(issues, ERROR, "RECURRING_AMOUNT_INVALID", "RECURRING_EXPENSE", id, "Valor padrão ausente ou negativo.");
            }
            invalidDate(expense.lastPaidOn(), issues, "RECURRING_LAST_PAYMENT_DATE_INVALID", "RECURRING_EXPENSE", id);
        }
        for (LegacySnapshot.ExpensePaymentRow payment : snapshot.expensePayments()) {
            String id = Long.toString(payment.id());
            if (!expenseIds.contains(payment.expenseId())) {
                add(issues, ERROR, "EXPENSE_PAYMENT_ORPHAN", "EXPENSE_PAYMENT", id, "Despesa referenciada não existe.");
            }
            if (payment.amount() == null || negative(payment.amount())) {
                add(issues, ERROR, "EXPENSE_PAYMENT_AMOUNT_INVALID", "EXPENSE_PAYMENT", id, "Valor ausente ou negativo.");
            }
            invalidDate(payment.paidOn(), issues, "EXPENSE_PAYMENT_DATE_INVALID", "EXPENSE_PAYMENT", id);
        }
    }

    private void invalidDate(String value, List<LegacyMigrationIssue> issues, String code, String type, String id) {
        if (value != null && !value.isBlank() && LegacyValueNormalizer.date(value).isEmpty()) {
            add(issues, ERROR, code, type, id, "Data não reconhecida; valor não incluído por segurança.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean negative(Integer value) {
        return value != null && value < 0;
    }

    private boolean negative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private void add(List<LegacyMigrationIssue> issues, LegacyMigrationIssue.Severity severity, String code,
                     String entityType, String legacyId, String message) {
        issues.add(new LegacyMigrationIssue(severity, code, entityType, legacyId, message));
    }
}
