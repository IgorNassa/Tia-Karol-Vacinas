package br.com.tiakarol.api.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_guardians")
class PatientGuardian {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "identity_number", nullable = false)
    private String identityNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PatientGuardian() {
    }

    PatientGuardian(String fullName, String identityNumber) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.identityNumber = identityNumber;
        this.createdAt = OffsetDateTime.now();
    }

    void attachTo(Patient patient) { this.patient = patient; }
    String getFullName() { return fullName; }
    String getIdentityNumber() { return identityNumber; }
}
