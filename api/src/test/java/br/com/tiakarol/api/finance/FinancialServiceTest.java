package br.com.tiakarol.api.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.payment.PaymentMethod;
import br.com.tiakarol.api.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private FinancialEntryRepository entryRepository;
    @Mock
    private RecurringExpenseRepository recurringRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditService auditService;
    private FinancialService service;

    @BeforeEach
    void setUp() {
        service = new FinancialService(entryRepository, recurringRepository, currentUser, auditService);
    }

    @Test
    void createsImmutableIncomeEntry() {
        when(currentUser.id()).thenReturn(USER_ID);
        when(entryRepository.save(any(FinancialEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialEntryResponse response = service.createEntry(new FinancialEntryRequest(FinancialEntryType.INCOME,
                FinancialCategory.CASH_DEPOSIT, "Troco inicial", new BigDecimal("200.00"), LocalDate.now(),
                PaymentMethod.CASH, "Abertura do caixa"));

        assertThat(response.type()).isEqualTo(FinancialEntryType.INCOME);
        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.active()).isTrue();
        verify(auditService).log("FINANCIAL_ENTRY", response.id(), "FINANCIAL_ENTRY_CREATED", null, response, null);
    }

    @Test
    void rejectsCategoryThatDoesNotMatchEntryType() {
        assertThatThrownBy(() -> service.createEntry(new FinancialEntryRequest(FinancialEntryType.INCOME,
                FinancialCategory.CASH_WITHDRAWAL, "Sangria", new BigDecimal("50.00"), LocalDate.now(),
                PaymentMethod.CASH, null)))
                .isInstanceOf(FinancialDomainException.class)
                .hasMessageContaining("categoria");

        verify(entryRepository, never()).save(any());
    }

    @Test
    void voidsEntryWithoutDeletingHistory() {
        FinancialEntry entry = new FinancialEntry(new FinancialEntryRequest(FinancialEntryType.EXPENSE,
                FinancialCategory.OTHER_EXPENSE, "Material", new BigDecimal("30.00"), LocalDate.now(),
                PaymentMethod.CASH, null), USER_ID);
        when(entryRepository.findByIdForUpdate(entry.getId())).thenReturn(Optional.of(entry));

        FinancialEntryResponse response = service.voidEntry(entry.getId(), "Lançamento duplicado");

        assertThat(response.active()).isFalse();
        assertThat(response.voidReason()).isEqualTo("Lançamento duplicado");
        verify(entryRepository, never()).delete(any(FinancialEntry.class));
    }

    @Test
    void paysRecurringExpenseAsLinkedExpenseEntry() {
        RecurringExpense expense = new RecurringExpense(new RecurringExpenseRequest("Aluguel",
                new BigDecimal("1500.00"), false, 10), USER_ID);
        when(recurringRepository.findByIdForUpdate(expense.getId())).thenReturn(Optional.of(expense));
        when(currentUser.id()).thenReturn(USER_ID);
        when(entryRepository.save(any(FinancialEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialEntryResponse response = service.payRecurring(expense.getId(),
                new RecurringExpensePaymentRequest(new BigDecimal("1500.00"), LocalDate.now(),
                        PaymentMethod.CASH, "Pago"));

        assertThat(response.type()).isEqualTo(FinancialEntryType.EXPENSE);
        assertThat(response.category()).isEqualTo(FinancialCategory.RECURRING_EXPENSE);
        assertThat(response.recurringExpenseId()).isEqualTo(expense.getId());
    }

    @Test
    void pendingMethodCannotRecordRealizedRecurringExpense() {
        assertThatThrownBy(() -> service.payRecurring(UUID.randomUUID(),
                new RecurringExpensePaymentRequest(BigDecimal.TEN, LocalDate.now(), PaymentMethod.PENDING, null)))
                .isInstanceOf(FinancialDomainException.class)
                .hasMessageContaining("pendente");

        verify(recurringRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void fixedExpenseRequiresExactConfiguredAmount() {
        RecurringExpense expense = new RecurringExpense(new RecurringExpenseRequest("Aluguel",
                new BigDecimal("1500.00"), false, 10), USER_ID);
        when(recurringRepository.findByIdForUpdate(expense.getId())).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.payRecurring(expense.getId(),
                new RecurringExpensePaymentRequest(new BigDecimal("1400.00"), LocalDate.now(),
                        PaymentMethod.CASH, null)))
                .isInstanceOf(FinancialDomainException.class)
                .hasMessageContaining("igual ao valor");

        verify(entryRepository, never()).save(any());
    }

    @Test
    void recurringExpenseCannotBePaidTwiceInSameMonth() {
        RecurringExpense expense = new RecurringExpense(new RecurringExpenseRequest("Internet",
                new BigDecimal("120.00"), true, 15), USER_ID);
        when(recurringRepository.findByIdForUpdate(expense.getId())).thenReturn(Optional.of(expense));
        when(entryRepository.existsByRecurringExpenseIdAndActiveTrueAndOccurredOnBetween(
                any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.payRecurring(expense.getId(),
                new RecurringExpensePaymentRequest(new BigDecimal("125.00"), LocalDate.now(),
                        PaymentMethod.CASH, null)))
                .isInstanceOf(FinancialDomainException.class)
                .hasMessageContaining("neste mês");

        verify(entryRepository, never()).save(any());
    }
}
