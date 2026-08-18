package br.com.tiakarol.api.stock;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "vaccine_lots")
class VaccineLot {
    @Id
    private UUID id;
    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName;
    @Column(name = "vaccine_type")
    private String vaccineType;
    @Column(name = "lot_code", nullable = false)
    private String lotCode;
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;
    private String manufacturer;
    private String supplier;
    @Column(name = "invoice_number")
    private String invoiceNumber;
    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;
    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;
    private String notes;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @OneToOne(mappedBy = "vaccineLot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private StockBalance balance;

    protected VaccineLot() { }

    VaccineLot(VaccineLotRequest request) {
        this.id = UUID.randomUUID();
        this.vaccineName = request.vaccineName().trim();
        this.vaccineType = trimToNull(request.vaccineType());
        this.lotCode = request.lotCode().trim();
        this.expirationDate = request.expirationDate();
        this.manufacturer = trimToNull(request.manufacturer());
        this.supplier = trimToNull(request.supplier());
        this.invoiceNumber = trimToNull(request.invoiceNumber());
        this.purchasePrice = request.purchasePrice();
        this.salePrice = request.salePrice();
        this.notes = trimToNull(request.notes());
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        this.balance = new StockBalance(this, request.initialQuantity());
    }

    UUID getId() { return id; }
    String getVaccineName() { return vaccineName; }
    String getVaccineType() { return vaccineType; }
    String getLotCode() { return lotCode; }
    LocalDate getExpirationDate() { return expirationDate; }
    String getManufacturer() { return manufacturer; }
    String getSupplier() { return supplier; }
    String getInvoiceNumber() { return invoiceNumber; }
    BigDecimal getPurchasePrice() { return purchasePrice; }
    BigDecimal getSalePrice() { return salePrice; }
    String getNotes() { return notes; }
    boolean isActive() { return active; }
    StockBalance getBalance() { return balance; }

    boolean matches(VaccineLotRequest request) {
        return Objects.equals(vaccineType, trimToNull(request.vaccineType()))
                && expirationDate.equals(request.expirationDate())
                && Objects.equals(manufacturer, trimToNull(request.manufacturer()))
                && Objects.equals(supplier, trimToNull(request.supplier()))
                && Objects.equals(invoiceNumber, trimToNull(request.invoiceNumber()))
                && purchasePrice.compareTo(request.purchasePrice()) == 0
                && salePrice.compareTo(request.salePrice()) == 0;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
