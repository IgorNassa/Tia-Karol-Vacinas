package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.NotBlank;

record ApplyAppointmentRequest(
        @NotBlank String applicationLocation,
        String reactions,
        String notes) {
}
