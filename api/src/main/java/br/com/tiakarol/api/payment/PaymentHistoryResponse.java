package br.com.tiakarol.api.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentHistoryResponse(UUID appointmentId, List<Entry> entries) {
    public record Entry(UUID id, PaymentMethod method, BigDecimal amount, boolean active,
                        OffsetDateTime receivedAt, OffsetDateTime createdAt,
                        OffsetDateTime voidedAt, String voidReason, String legacyMethod) { }
}
