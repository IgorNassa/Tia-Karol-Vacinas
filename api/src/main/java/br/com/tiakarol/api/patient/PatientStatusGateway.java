package br.com.tiakarol.api.patient;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PatientStatusGateway {
    private final PatientRepository repository;

    PatientStatusGateway(PatientRepository repository) {
        this.repository = repository;
    }

    public void requireActive(UUID patientId) {
        Patient patient = repository.findById(patientId)
                .orElseThrow(() -> new PatientDomainException("Paciente não encontrado."));
        if (!patient.isActive()) {
            throw new PatientDomainException("Paciente inativo não pode receber novo agendamento.");
        }
    }
}
