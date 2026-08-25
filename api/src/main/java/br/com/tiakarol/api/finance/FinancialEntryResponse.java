package br.com.tiakarol.api.finance;

import br.com.tiakarol.api.payment.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FinancialEntryResponse(UUID id, FinancialEntryType type, FinancialCategory category,
                                     String description, BigDecimal amount, LocalDate occurredOn,
                                     PaymentMethod paymentMethod, String notes, UUID recurringExpenseId,
                                     boolean active, OffsetDateTime voidedAt, String voidReason,
                                     OffsetDateTime createdAt) { }
