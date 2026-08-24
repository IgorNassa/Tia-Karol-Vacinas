package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

record UpdateAppointmentRequest(
        UUID patientId,
        @DecimalMin("0.00") BigDecimal grossAmount,
        @DecimalMin("0.00") BigDecimal discountAmount,
        @Size(max = 5000) String notes,
        @Size(max = 100) String applicationLocation,
        @Size(max = 5000) String reactions) {
    boolean isEmpty() {
        return patientId == null && grossAmount == null && discountAmount == null && notes == null
                && applicationLocation == null && reactions == null;
    }
}
