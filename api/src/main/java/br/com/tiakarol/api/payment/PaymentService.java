package br.com.tiakarol.api.payment;

import br.com.tiakarol.api.appointment.AppointmentBillingGateway;
import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.security.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentService {
    private final PaymentEntryRepository repository;
    private final AppointmentBillingGateway billingGateway;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    PaymentService(PaymentEntryRepository repository, AppointmentBillingGateway billingGateway,
                   CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.billingGateway = billingGateway;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    PaymentCartResponse get(UUID appointmentId) {
        var billing = billingGateway.getBilling(appointmentId);
        return response(billing.expectedAmount(), appointmentId, activeEntries(appointmentId), false);
    }

    @Transactional(readOnly = true)
    PaymentHistoryResponse history(UUID appointmentId) {
        billingGateway.getBilling(appointmentId);
        List<PaymentHistoryResponse.Entry> entries = repository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId)
                .stream().map(entry -> new PaymentHistoryResponse.Entry(entry.getId(), entry.getMethod(),
                        entry.getAmount(), entry.isActive(), entry.getReceivedAt(), entry.getCreatedAt(),
                        entry.getVoidedAt(), entry.getVoidReason(), entry.getLegacyMethod())).toList();
        return new PaymentHistoryResponse(appointmentId, entries);
    }

    @Transactional
    PaymentCartResponse replace(UUID appointmentId, PaymentCartRequest request) {
        var billing = billingGateway.lockForPayment(appointmentId);
        validate(request, billing.expectedAmount());
        List<PaymentEntry> previous = activeEntries(appointmentId);
        PaymentCartResponse before = response(billing.expectedAmount(), appointmentId, previous, false);
        previous.forEach(entry -> entry.voidEntry("Carrinho de pagamento substituído."));
        if (!previous.isEmpty()) repository.flush();
        List<PaymentEntry> entries = request.payments().stream()
                .map(item -> new PaymentEntry(appointmentId, item.method(), item.amount(), currentUser.id()))
                .toList();
        repository.saveAll(entries);
        PaymentCartResponse after = response(billing.expectedAmount(), appointmentId, entries, false);
        auditService.log("PAYMENT_CART", appointmentId, "PAYMENT_CART_REPLACED", before, after,
                previous.isEmpty() ? "Primeiro registro do pagamento." : "Carrinho anterior preservado no histórico.");
        return after;
    }

    @Transactional
    PaymentCartResponse voidCart(UUID appointmentId, String reason) {
        requireReason(reason);
        var billing = billingGateway.lockForUpdate(appointmentId);
        List<PaymentEntry> entries = activeEntries(appointmentId);
        if (entries.isEmpty()) {
            throw new PaymentDomainException("Não há pagamento ativo para estornar.");
        }
        PaymentCartResponse before = response(billing.expectedAmount(), appointmentId, entries, false);
        entries.forEach(entry -> entry.voidEntry(reason));
        PaymentCartResponse after = response(billing.expectedAmount(), appointmentId, List.of(), true);
        auditService.log("PAYMENT_CART", appointmentId, "PAYMENT_CART_VOIDED", before, after, reason.trim());
        return after;
    }

    private void validate(PaymentCartRequest request, BigDecimal expectedAmount) {
        BigDecimal total = request.payments().stream().map(PaymentCartRequest.PaymentItemRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(expectedAmount) != 0) {
            throw new PaymentDomainException("A soma dos pagamentos deve ser igual ao valor final do atendimento.");
        }
        long pendingEntries = request.payments().stream()
                .filter(item -> item.method() == PaymentMethod.PENDING).count();
        if (pendingEntries > 1) {
            throw new PaymentDomainException("O carrinho aceita no máximo um lançamento pendente.");
        }
        long distinctMethods = request.payments().stream().map(PaymentCartRequest.PaymentItemRequest::method)
                .distinct().count();
        if (distinctMethods != request.payments().size()) {
            throw new PaymentDomainException("Cada forma de pagamento pode aparecer somente uma vez no carrinho.");
        }
    }

    private List<PaymentEntry> activeEntries(UUID appointmentId) {
        return repository.findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(appointmentId);
    }

    private PaymentCartResponse response(BigDecimal expectedAmount, UUID appointmentId,
                                         List<PaymentEntry> entries, boolean voided) {
        BigDecimal total = entries.stream().map(PaymentEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = entries.stream().filter(entry -> entry.getMethod() == PaymentMethod.PENDING)
                .map(PaymentEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = total.subtract(pending);
        PaymentStatus status;
        if (voided) {
            status = PaymentStatus.VOIDED;
        } else if (total.compareTo(expectedAmount) < 0 || entries.stream()
                .anyMatch(entry -> entry.getMethod() == PaymentMethod.PENDING)) {
            status = PaymentStatus.PENDING;
        } else {
            status = PaymentStatus.PAID;
        }
        List<PaymentCartResponse.PaymentItemResponse> items = entries.stream()
                .map(entry -> new PaymentCartResponse.PaymentItemResponse(entry.getId(), entry.getMethod(),
                        entry.getAmount(), entry.getReceivedAt(), entry.getCreatedAt()))
                .toList();
        return new PaymentCartResponse(appointmentId, expectedAmount, total, received, pending, status, items);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new PaymentDomainException("O motivo do estorno é obrigatório.");
        }
    }
}
