package br.com.tiakarol.api.finance;

import br.com.tiakarol.api.payment.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_entries")
class FinancialEntry {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private FinancialEntryType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialCategory category;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    private String notes;
    @Column(name = "recurring_expense_id")
    private UUID recurringExpenseId;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;
    @Column(name = "void_reason")
    private String voidReason;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FinancialEntry() { }

    FinancialEntry(FinancialEntryRequest request, UUID createdBy) {
        this(request.type(), request.category(), request.description(), request.amount(), request.occurredOn(),
                request.paymentMethod(), request.notes(), null, createdBy);
    }

    FinancialEntry(FinancialEntryType type, FinancialCategory category, String description, BigDecimal amount,
                   LocalDate occurredOn, PaymentMethod paymentMethod, String notes, UUID recurringExpenseId,
                   UUID createdBy) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.category = category;
        this.description = description.trim();
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.paymentMethod = paymentMethod;
        this.notes = trimToNull(notes);
        this.recurringExpenseId = recurringExpenseId;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    UUID getId() { return id; }
    FinancialEntryType getType() { return type; }
    FinancialCategory getCategory() { return category; }
    String getDescription() { return description; }
    BigDecimal getAmount() { return amount; }
    LocalDate getOccurredOn() { return occurredOn; }
    PaymentMethod getPaymentMethod() { return paymentMethod; }
    String getNotes() { return notes; }
    UUID getRecurringExpenseId() { return recurringExpenseId; }
    boolean isActive() { return active; }
    OffsetDateTime getVoidedAt() { return voidedAt; }
    String getVoidReason() { return voidReason; }
    OffsetDateTime getCreatedAt() { return createdAt; }

    void voidEntry(String reason) {
        if (!active) throw new FinancialDomainException("O lançamento já está estornado.");
        active = false;
        voidedAt = OffsetDateTime.now();
        voidReason = reason.trim();
        updatedAt = voidedAt;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
