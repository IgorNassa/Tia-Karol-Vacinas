package br.com.tiakarol.api.appointment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID vaccineLotId,
        OffsetDateTime scheduledAt,
        AppointmentStatus status,
        ReservationStatus reservationStatus,
        String applicationLocation,
        String reactions,
        String notes,
        String cancellationReason,
        String reservationResolutionReason,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        OffsetDateTime appliedAt) {
}
