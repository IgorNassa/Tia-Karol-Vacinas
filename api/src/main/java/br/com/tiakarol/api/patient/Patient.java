package br.com.tiakarol.api.patient;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients")
class Patient {
    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    private PatientIdentityType identityType;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String allergies;

    @Column(name = "allergies_confirmed", nullable = false)
    private boolean allergiesConfirmed;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "inactivated_at")
    private OffsetDateTime inactivatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PatientAddress address;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientGuardian> guardians = new ArrayList<>();

    protected Patient() {
    }

    Patient(UUID id, String fullName, PatientIdentityType identityType, String identityNumber,
            LocalDate birthDate, String phone, String allergies, boolean allergiesConfirmed) {
        this.id = id;
        this.fullName = fullName;
        this.identityType = identityType;
        this.identityNumber = identityNumber;
        this.birthDate = birthDate;
        this.phone = phone;
        this.allergies = allergies;
        this.allergiesConfirmed = allergiesConfirmed;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    UUID getId() { return id; }
    String getFullName() { return fullName; }
    PatientIdentityType getIdentityType() { return identityType; }
    String getIdentityNumber() { return identityNumber; }
    LocalDate getBirthDate() { return birthDate; }
    String getPhone() { return phone; }
    String getAllergies() { return allergies; }
    boolean isAllergiesConfirmed() { return allergiesConfirmed; }
    boolean isActive() { return active; }
    OffsetDateTime getInactivatedAt() { return inactivatedAt; }
    PatientAddress getAddress() { return address; }
    List<PatientGuardian> getGuardians() { return guardians; }

    void replaceAddress(PatientAddress newAddress) {
        newAddress.attachTo(this);
        this.address = newAddress;
    }

    void replaceGuardians(List<PatientGuardian> newGuardians) {
        guardians.clear();
        newGuardians.forEach(guardian -> {
            guardian.attachTo(this);
            guardians.add(guardian);
        });
    }

    void update(String fullName, PatientIdentityType identityType, String identityNumber, LocalDate birthDate,
                String phone, String allergies, boolean allergiesConfirmed) {
        this.fullName = fullName;
        this.identityType = identityType;
        this.identityNumber = identityNumber;
        this.birthDate = birthDate;
        this.phone = phone;
        this.allergies = allergies;
        this.allergiesConfirmed = allergiesConfirmed;
        this.updatedAt = OffsetDateTime.now();
    }

    void inactivate() {
        this.active = false;
        this.inactivatedAt = OffsetDateTime.now();
        this.updatedAt = this.inactivatedAt;
    }
}
