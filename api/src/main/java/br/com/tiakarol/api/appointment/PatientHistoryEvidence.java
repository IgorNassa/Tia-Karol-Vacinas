package br.com.tiakarol.api.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PatientHistoryEvidence(long appointmentCount, long applicationCount, List<Item> recentHistory) {
    public boolean hasHistory() {
        return appointmentCount > 0;
    }

    public record Item(UUID appointmentId, AppointmentStatus status, OffsetDateTime scheduledAt,
                       OffsetDateTime appliedAt, UUID vaccineLotId) { }
}
