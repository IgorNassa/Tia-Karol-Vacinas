package br.com.tiakarol.api.patient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record PatientResponse(UUID id, String fullName, PatientIdentityType identityType, String identityNumber,
                       LocalDate birthDate, String phone, String allergies, boolean allergiesConfirmed,
                       boolean active, OffsetDateTime inactivatedAt, AddressResponse address,
                       List<GuardianResponse> guardians) {
    record AddressResponse(String postalCode, String street, String number, String complement, String district,
                           String city, String state, String ibgeCode) { }
    record GuardianResponse(String fullName, String identityNumber) { }
}
