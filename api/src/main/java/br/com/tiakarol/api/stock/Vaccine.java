package br.com.tiakarol.api.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccines")
class Vaccine {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(name = "vaccine_type") private String vaccineType;
    private String manufacturer;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected Vaccine() { }

    Vaccine(VaccineRequest request) {
        id = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        update(request);
        createdAt = updatedAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getVaccineType() { return vaccineType; }
    String getManufacturer() { return manufacturer; }
    boolean isActive() { return active; }

    void update(VaccineRequest request) {
        name = request.name().trim();
        vaccineType = trimToNull(request.vaccineType());
        manufacturer = trimToNull(request.manufacturer());
        updatedAt = OffsetDateTime.now();
    }

    void inactivate() {
        active = false;
        updatedAt = OffsetDateTime.now();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
