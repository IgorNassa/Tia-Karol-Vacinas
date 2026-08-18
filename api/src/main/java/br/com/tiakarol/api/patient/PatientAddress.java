package br.com.tiakarol.api.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "patient_addresses")
class PatientAddress {
    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    @Column(nullable = false)
    private String street;
    @Column(nullable = false)
    private String number;
    private String complement;
    @Column(name = "district", nullable = false)
    private String district;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String state;
    @Column(name = "ibge_code")
    private String ibgeCode;

    protected PatientAddress() {
    }

    PatientAddress(String postalCode, String street, String number, String complement, String district,
                   String city, String state, String ibgeCode) {
        this.postalCode = postalCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.district = district;
        this.city = city;
        this.state = state;
        this.ibgeCode = ibgeCode;
    }

    void attachTo(Patient patient) { this.patient = patient; }
    String getPostalCode() { return postalCode; }
    String getStreet() { return street; }
    String getNumber() { return number; }
    String getComplement() { return complement; }
    String getDistrict() { return district; }
    String getCity() { return city; }
    String getState() { return state; }
    String getIbgeCode() { return ibgeCode; }
}
