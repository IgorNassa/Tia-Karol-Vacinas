package br.com.tiakarol.api.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

record PatientRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotNull PatientIdentityType identityType,
        @Size(max = 50) String identityNumber,
        @NotNull @PastOrPresent LocalDate birthDate,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank String allergies,
        boolean allergiesConfirmed,
        @NotNull @Valid AddressRequest address,
        @Valid List<GuardianRequest> guardians) {

    record AddressRequest(
            @NotBlank @Size(max = 12) String postalCode,
            @NotBlank String street,
            @NotBlank String number,
            String complement,
            @NotBlank String district,
            @NotBlank String city,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String state,
            String ibgeCode) { }

    record GuardianRequest(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Size(max = 50) String identityNumber) { }
}
