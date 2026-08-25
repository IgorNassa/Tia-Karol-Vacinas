package br.com.tiakarol.api.finance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RecurringExpenseResponse(UUID id, String name, BigDecimal defaultAmount,
                                       boolean variableAmount, int dueDay, boolean active,
                                       OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
