package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record CreateAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID vaccineLotId,
        @NotNull @Future OffsetDateTime scheduledAt,
        @NotNull @DecimalMin("0.00") BigDecimal grossAmount,
        @NotNull @DecimalMin("0.00") BigDecimal discountAmount,
        String notes) {
}
