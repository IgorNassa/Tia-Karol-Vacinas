package br.com.tiakarol.api.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_entries")
class PaymentEntry {
    @Id
    private UUID id;
    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod method;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;
    @Column(name = "void_reason")
    private String voidReason;
    @Column(name = "received_at")
    private OffsetDateTime receivedAt;
    @Column(name = "legacy_method")
    private String legacyMethod;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PaymentEntry() { }

    PaymentEntry(UUID appointmentId, PaymentMethod method, BigDecimal amount, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.appointmentId = appointmentId;
        this.method = method;
        this.amount = amount;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        this.receivedAt = method == PaymentMethod.PENDING ? null : this.createdAt;
    }

    UUID getId() { return id; }
    PaymentMethod getMethod() { return method; }
    BigDecimal getAmount() { return amount; }
    OffsetDateTime getCreatedAt() { return createdAt; }
    boolean isActive() { return active; }
    OffsetDateTime getVoidedAt() { return voidedAt; }
    String getVoidReason() { return voidReason; }
    OffsetDateTime getReceivedAt() { return receivedAt; }
    String getLegacyMethod() { return legacyMethod; }

    void voidEntry(String reason) {
        if (!active) {
            return;
        }
        active = false;
        voidedAt = OffsetDateTime.now();
        voidReason = reason.trim();
        updatedAt = voidedAt;
    }
}
