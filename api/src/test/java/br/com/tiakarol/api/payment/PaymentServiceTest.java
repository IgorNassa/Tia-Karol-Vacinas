package br.com.tiakarol.api.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.appointment.AppointmentBillingGateway;
import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.security.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final UUID APPOINTMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final BigDecimal EXPECTED_AMOUNT = new BigDecimal("150.00");

    @Mock
    private PaymentEntryRepository repository;
    @Mock
    private AppointmentBillingGateway billingGateway;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditService auditService;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, billingGateway, currentUser, auditService);
    }

    @Test
    void registersSplitPaymentWhenSumMatchesAppointmentAmount() {
        payableForUpdate();
        when(currentUser.id()).thenReturn(USER_ID);
        when(repository.findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(APPOINTMENT_ID)).thenReturn(List.of());

        PaymentCartResponse response = service.replace(APPOINTMENT_ID, request(
                item(PaymentMethod.CREDIT_CARD, "100.00"),
                item(PaymentMethod.CASH, "50.00")));

        assertThat(response.registeredAmount()).isEqualByComparingTo("150.00");
        assertThat(response.receivedAmount()).isEqualByComparingTo("150.00");
        assertThat(response.pendingAmount()).isZero();
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.payments()).hasSize(2);
        verify(repository).saveAll(any());
        verify(auditService).log(eq("PAYMENT_CART"), eq(APPOINTMENT_ID), eq("PAYMENT_CART_REPLACED"),
                any(), any(), any());
    }

    @Test
    void flushesVoidedCartBeforeInsertingReplacement() {
        payableForUpdate();
        when(currentUser.id()).thenReturn(USER_ID);
        PaymentEntry previous = new PaymentEntry(APPOINTMENT_ID, PaymentMethod.PENDING, EXPECTED_AMOUNT, USER_ID);
        when(repository.findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(APPOINTMENT_ID))
                .thenReturn(List.of(previous));

        service.replace(APPOINTMENT_ID, request(item(PaymentMethod.CASH, "150.00")));

        assertThat(previous.isActive()).isFalse();
        verify(repository).flush();
        verify(repository).saveAll(any());
    }

    @Test
    void marksCartPendingWhenRemainderWasExplicitlyDeferred() {
        payableForUpdate();
        when(currentUser.id()).thenReturn(USER_ID);
        when(repository.findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(APPOINTMENT_ID)).thenReturn(List.of());

        PaymentCartResponse response = service.replace(APPOINTMENT_ID, request(
                item(PaymentMethod.DEBIT_CARD, "100.00"),
                item(PaymentMethod.PENDING, "50.00")));

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.receivedAmount()).isEqualByComparingTo("100.00");
        assertThat(response.pendingAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejectsCartWhoseSumDiffersFromAppointmentAmount() {
        payableForUpdate();

        assertThatThrownBy(() -> service.replace(APPOINTMENT_ID,
                request(item(PaymentMethod.CASH, "149.99"))))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("A soma dos pagamentos deve ser igual ao valor final do atendimento.");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void rejectsMoreThanOnePendingEntry() {
        payableForUpdate();

        assertThatThrownBy(() -> service.replace(APPOINTMENT_ID, request(
                item(PaymentMethod.PENDING, "100.00"),
                item(PaymentMethod.PENDING, "50.00"))))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("O carrinho aceita no máximo um lançamento pendente.");
    }

    @Test
    void rejectsDuplicatePaymentMethod() {
        payableForUpdate();

        assertThatThrownBy(() -> service.replace(APPOINTMENT_ID, request(
                item(PaymentMethod.CASH, "100.00"), item(PaymentMethod.CASH, "50.00"))))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("somente uma vez");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void exposesCompletePaymentHistoryIncludingVoidedEntries() {
        PaymentEntry entry = new PaymentEntry(APPOINTMENT_ID, PaymentMethod.CASH, EXPECTED_AMOUNT, USER_ID);
        entry.voidEntry("Correção");
        when(billingGateway.getBilling(APPOINTMENT_ID))
                .thenReturn(new AppointmentBillingGateway.BillingDetails(APPOINTMENT_ID, EXPECTED_AMOUNT));
        when(repository.findByAppointmentIdOrderByCreatedAtAsc(APPOINTMENT_ID)).thenReturn(List.of(entry));

        PaymentHistoryResponse response = service.history(APPOINTMENT_ID);

        assertThat(response.entries()).singleElement().satisfies(history -> {
            assertThat(history.active()).isFalse();
            assertThat(history.voidReason()).isEqualTo("Correção");
            assertThat(history.receivedAt()).isNotNull();
        });
    }

    @Test
    void voidsActiveCartWithMandatoryReason() {
        billableForVoid();
        PaymentEntry entry = new PaymentEntry(APPOINTMENT_ID, PaymentMethod.CASH, EXPECTED_AMOUNT, USER_ID);
        when(repository.findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(APPOINTMENT_ID))
                .thenReturn(List.of(entry));

        PaymentCartResponse response = service.voidCart(APPOINTMENT_ID, "Pagamento lançado incorretamente");

        assertThat(response.status()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(response.payments()).isEmpty();
        verify(auditService).log(eq("PAYMENT_CART"), eq(APPOINTMENT_ID), eq("PAYMENT_CART_VOIDED"),
                any(), any(), any());
    }

    @Test
    void requiresReasonBeforeLoadingCartForVoid() {
        assertThatThrownBy(() -> service.voidCart(APPOINTMENT_ID, " "))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("O motivo do estorno é obrigatório.");

        verify(billingGateway, never()).lockForUpdate(any());
    }

    private void payableForUpdate() {
        when(billingGateway.lockForPayment(APPOINTMENT_ID))
                .thenReturn(new AppointmentBillingGateway.BillingDetails(APPOINTMENT_ID, EXPECTED_AMOUNT));
    }

    private void billableForVoid() {
        when(billingGateway.lockForUpdate(APPOINTMENT_ID))
                .thenReturn(new AppointmentBillingGateway.BillingDetails(APPOINTMENT_ID, EXPECTED_AMOUNT));
    }

    private PaymentCartRequest request(PaymentCartRequest.PaymentItemRequest... items) {
        return new PaymentCartRequest(List.of(items));
    }

    private PaymentCartRequest.PaymentItemRequest item(PaymentMethod method, String amount) {
        return new PaymentCartRequest.PaymentItemRequest(method, new BigDecimal(amount));
    }
}
