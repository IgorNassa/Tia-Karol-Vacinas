package br.com.tiakarol.api.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
class StockMovement {
    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "vaccine_lot_id", nullable = false)
    private VaccineLot vaccineLot;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private StockMovementType movementType;
    @Column(nullable = false)
    private int quantity;
    private String reason;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StockMovement() { }

    StockMovement(VaccineLot vaccineLot, StockMovementType movementType, int quantity, String reason, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.vaccineLot = vaccineLot;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
    }
}
