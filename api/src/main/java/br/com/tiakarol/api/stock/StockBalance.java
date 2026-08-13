package br.com.tiakarol.api.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_balances")
class StockBalance {
    @Id
    @Column(name = "vaccine_lot_id")
    private UUID vaccineLotId;
    @MapsId
    @OneToOne
    @JoinColumn(name = "vaccine_lot_id")
    private VaccineLot vaccineLot;
    @Column(name = "physical_quantity", nullable = false)
    private int physicalQuantity;
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StockBalance() { }

    StockBalance(VaccineLot vaccineLot, int initialQuantity) {
        this.vaccineLot = vaccineLot;
        this.physicalQuantity = initialQuantity;
        this.reservedQuantity = 0;
        this.updatedAt = OffsetDateTime.now();
    }

    int getPhysicalQuantity() { return physicalQuantity; }
    int getReservedQuantity() { return reservedQuantity; }
    int getAvailableQuantity() { return physicalQuantity - reservedQuantity; }

    void addPhysical(int quantity) {
        physicalQuantity += quantity;
        updatedAt = OffsetDateTime.now();
    }

    void removePhysical(int quantity) {
        if (quantity > getAvailableQuantity()) {
            throw new StockDomainException("Saldo disponível insuficiente para esta operação.");
        }
        physicalQuantity -= quantity;
        updatedAt = OffsetDateTime.now();
    }
}
