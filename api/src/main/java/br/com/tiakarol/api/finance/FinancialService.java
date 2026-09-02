package br.com.tiakarol.api.finance;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.payment.PaymentMethod;
import br.com.tiakarol.api.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinancialService {
    private final FinancialEntryRepository entryRepository;
    private final RecurringExpenseRepository recurringRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    FinancialService(FinancialEntryRepository entryRepository, RecurringExpenseRepository recurringRepository,
                     CurrentUser currentUser, AuditService auditService) {
        this.entryRepository = entryRepository;
        this.recurringRepository = recurringRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional
    FinancialEntryResponse createEntry(FinancialEntryRequest request) {
        validateEntry(request.type(), request.category(), request.paymentMethod());
        FinancialEntry entry = entryRepository.save(new FinancialEntry(request, currentUser.id()));
        FinancialEntryResponse response = toResponse(entry);
        auditService.log("FINANCIAL_ENTRY", entry.getId(), "FINANCIAL_ENTRY_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    Page<FinancialEntryResponse> listEntries(FinancialEntryType type, FinancialCategory category,
                                             LocalDate fromDate, LocalDate toDate, Boolean active,
                                             Pageable pageable) {
        validatePeriod(fromDate, toDate);
        Specification<FinancialEntry> filters = (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) predicates.add(criteria.equal(root.get("type"), type));
            if (category != null) predicates.add(criteria.equal(root.get("category"), category));
            if (fromDate != null) predicates.add(criteria.greaterThanOrEqualTo(root.get("occurredOn"), fromDate));
            if (toDate != null) predicates.add(criteria.lessThanOrEqualTo(root.get("occurredOn"), toDate));
            if (active != null) predicates.add(criteria.equal(root.get("active"), active));
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
        return entryRepository.findAll(filters, pageable).map(this::toResponse);
    }

    @Transactional
    FinancialEntryResponse voidEntry(UUID id, String reason) {
        requireReason(reason);
        FinancialEntry entry = entryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new FinancialDomainException("Lançamento financeiro não encontrado."));
        FinancialEntryResponse before = toResponse(entry);
        entry.voidEntry(reason);
        FinancialEntryResponse response = toResponse(entry);
        auditService.log("FINANCIAL_ENTRY", id, "FINANCIAL_ENTRY_VOIDED", before, response, reason.trim());
        return response;
    }

    @Transactional
    RecurringExpenseResponse createRecurring(RecurringExpenseRequest request) {
        RecurringExpense expense = recurringRepository.save(new RecurringExpense(request, currentUser.id()));
        RecurringExpenseResponse response = toResponse(expense);
        auditService.log("RECURRING_EXPENSE", expense.getId(), "RECURRING_EXPENSE_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    Page<RecurringExpenseResponse> listRecurring(Boolean active, Pageable pageable) {
        Page<RecurringExpense> expenses = active == null ? recurringRepository.findAll(pageable)
                : recurringRepository.findByActive(active, pageable);
        return expenses.map(this::toResponse);
    }

    @Transactional
    RecurringExpenseResponse updateRecurring(UUID id, RecurringExpenseRequest request) {
        RecurringExpense expense = recurringForUpdate(id);
        RecurringExpenseResponse before = toResponse(expense);
        expense.update(request);
        RecurringExpenseResponse response = toResponse(expense);
        auditService.log("RECURRING_EXPENSE", id, "RECURRING_EXPENSE_UPDATED", before, response, null);
        return response;
    }

    @Transactional
    RecurringExpenseResponse inactivateRecurring(UUID id) {
        RecurringExpense expense = recurringForUpdate(id);
        RecurringExpenseResponse before = toResponse(expense);
        expense.inactivate();
        RecurringExpenseResponse response = toResponse(expense);
        auditService.log("RECURRING_EXPENSE", id, "RECURRING_EXPENSE_INACTIVATED", before, response, null);
        return response;
    }

    @Transactional
    FinancialEntryResponse payRecurring(UUID id, RecurringExpensePaymentRequest request) {
        if (request.paymentMethod() == PaymentMethod.PENDING) {
            throw new FinancialDomainException("Pagamento pendente não pode registrar uma despesa realizada.");
        }
        RecurringExpense expense = recurringForUpdate(id);
        if (!expense.isActive()) throw new FinancialDomainException("Despesa recorrente inativa não pode ser paga.");
        if (!expense.isVariableAmount() && request.amount().compareTo(expense.getDefaultAmount()) != 0) {
            throw new FinancialDomainException("O valor pago deve ser igual ao valor da despesa fixa.");
        }
        LocalDate firstDay = request.occurredOn().withDayOfMonth(1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        if (entryRepository.existsByRecurringExpenseIdAndActiveTrueAndOccurredOnBetween(id, firstDay, lastDay)) {
            throw new FinancialDomainException("A despesa recorrente já possui pagamento ativo neste mês.");
        }
        FinancialEntry entry = entryRepository.save(new FinancialEntry(FinancialEntryType.EXPENSE,
                FinancialCategory.RECURRING_EXPENSE, expense.getName(), request.amount(), request.occurredOn(),
                request.paymentMethod(), request.notes(), expense.getId(), currentUser.id()));
        FinancialEntryResponse response = toResponse(entry);
        auditService.log("FINANCIAL_ENTRY", entry.getId(), "RECURRING_EXPENSE_PAID", null, response, null);
        return response;
    }

    private RecurringExpense recurringForUpdate(UUID id) {
        return recurringRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new FinancialDomainException("Despesa recorrente não encontrada."));
    }

    private void validateEntry(FinancialEntryType type, FinancialCategory category, PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.PENDING) {
            throw new FinancialDomainException("Lançamento realizado não aceita forma de pagamento pendente.");
        }
        if (category == FinancialCategory.LEGACY_IMPORT) {
            throw new FinancialDomainException("A categoria de importação é reservada para a migração do legado.");
        }
        boolean incomeCategory = category == FinancialCategory.OTHER_INCOME
                || category == FinancialCategory.CASH_DEPOSIT;
        boolean expenseCategory = category == FinancialCategory.OTHER_EXPENSE
                || category == FinancialCategory.CASH_WITHDRAWAL
                || category == FinancialCategory.RECURRING_EXPENSE;
        if ((type == FinancialEntryType.INCOME && expenseCategory)
                || (type == FinancialEntryType.EXPENSE && incomeCategory)
                || category == FinancialCategory.RECURRING_EXPENSE) {
            throw new FinancialDomainException("A categoria não corresponde ao tipo do lançamento manual.");
        }
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new FinancialDomainException("A data final deve ser igual ou posterior à data inicial.");
        }
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new FinancialDomainException("O motivo do estorno é obrigatório.");
        }
    }

    private FinancialEntryResponse toResponse(FinancialEntry entry) {
        return new FinancialEntryResponse(entry.getId(), entry.getType(), entry.getCategory(),
                entry.getDescription(), entry.getAmount(), entry.getOccurredOn(), entry.getPaymentMethod(),
                entry.getNotes(), entry.getRecurringExpenseId(), entry.isActive(), entry.getVoidedAt(),
                entry.getVoidReason(), entry.getCreatedAt());
    }

    private RecurringExpenseResponse toResponse(RecurringExpense expense) {
        return new RecurringExpenseResponse(expense.getId(), expense.getName(), expense.getDefaultAmount(),
                expense.isVariableAmount(), expense.getDueDay(), expense.isActive(), expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}
