package br.com.tiakarol.api.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentCartResponse(
        UUID appointmentId,
        BigDecimal expectedAmount,
        BigDecimal registeredAmount,
        PaymentStatus status,
        List<PaymentItemResponse> payments) {

    public record PaymentItemResponse(UUID id, PaymentMethod method, BigDecimal amount, OffsetDateTime createdAt) { }
}
