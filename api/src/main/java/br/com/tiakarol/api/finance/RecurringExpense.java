package br.com.tiakarol.api.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_expenses")
class RecurringExpense {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(name = "default_amount", nullable = false)
    private BigDecimal defaultAmount;
    @Column(name = "variable_amount", nullable = false)
    private boolean variableAmount;
    @Column(name = "due_day", nullable = false)
    private int dueDay;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RecurringExpense() { }

    RecurringExpense(RecurringExpenseRequest request, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        update(request);
        this.createdAt = this.updatedAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    BigDecimal getDefaultAmount() { return defaultAmount; }
    boolean isVariableAmount() { return variableAmount; }
    int getDueDay() { return dueDay; }
    boolean isActive() { return active; }
    OffsetDateTime getCreatedAt() { return createdAt; }
    OffsetDateTime getUpdatedAt() { return updatedAt; }

    void update(RecurringExpenseRequest request) {
        name = request.name().trim();
        defaultAmount = request.defaultAmount();
        variableAmount = request.variableAmount();
        dueDay = request.dueDay();
        updatedAt = OffsetDateTime.now();
    }

    void inactivate() {
        active = false;
        updatedAt = OffsetDateTime.now();
    }
}
