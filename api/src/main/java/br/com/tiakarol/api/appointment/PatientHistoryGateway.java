package br.com.tiakarol.api.appointment;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PatientHistoryGateway {
    private final AppointmentRepository repository;

    PatientHistoryGateway(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PatientHistoryEvidence summarize(UUID patientId) {
        return new PatientHistoryEvidence(repository.countByPatientId(patientId),
                repository.countByPatientIdAndStatus(patientId, AppointmentStatus.APPLIED),
                repository.findTop5ByPatientIdOrderByScheduledAtDesc(patientId).stream()
                        .map(item -> new PatientHistoryEvidence.Item(item.getId(), item.getStatus(),
                                item.getScheduledAt(), item.getAppliedAt(), item.getVaccineLotId()))
                        .toList());
    }
}
