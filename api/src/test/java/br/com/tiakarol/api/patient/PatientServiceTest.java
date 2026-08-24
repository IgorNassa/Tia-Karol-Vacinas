package br.com.tiakarol.api.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.appointment.PatientHistoryGateway;
import br.com.tiakarol.api.appointment.PatientHistoryEvidence;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository repository;
    @Mock
    private AuditService auditService;
    @Mock
    private PatientHistoryGateway patientHistory;

    private PatientService service;

    @BeforeEach
    void setUp() {
        service = new PatientService(repository, auditService, patientHistory);
    }

    @Test
    void createsPatientAndNormalizesCpf() {
        when(repository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = service.create(request(PatientIdentityType.CPF, "529.982.247-25", true,
                List.of()));

        assertThat(response.identityNumber()).isEqualTo("52998224725");
        assertThat(response.active()).isTrue();
        assertThat(response.address().state()).isEqualTo("SP");
        verify(auditService).log("PATIENT", response.id(), "PATIENT_CREATED", null, response, null);
    }

    @Test
    void rejectsInvalidCpfBeforePersistence() {
        assertThatThrownBy(() -> service.create(request(PatientIdentityType.CPF, "111.111.111-11", true,
                List.of())))
                .isInstanceOf(PatientDomainException.class)
                .hasMessage("CPF inválido.");

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDuplicateCpf() {
        when(repository.existsByIdentityTypeAndIdentityNumber(PatientIdentityType.CPF, "52998224725"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request(PatientIdentityType.CPF, "52998224725", true,
                List.of())))
                .isInstanceOf(PatientDomainException.class)
                .hasMessage("Já existe paciente com esse documento.");
    }

    @Test
    void requiresAllergyConfirmation() {
        assertThatThrownBy(() -> service.create(request(PatientIdentityType.CPF, "52998224725", false,
                List.of())))
                .isInstanceOf(PatientDomainException.class)
                .hasMessage("A confirmação das informações de alergia é obrigatória.");
    }

    @Test
    void requiresGuardianForNewborn() {
        assertThatThrownBy(() -> service.create(request(PatientIdentityType.NEWBORN, null, true, List.of())))
                .isInstanceOf(PatientDomainException.class)
                .hasMessage("Recém-nascido precisa de ao menos um responsável.");
    }

    @Test
    void acceptsNewbornWithGuardianAndWithoutOwnDocument() {
        when(repository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<PatientRequest.GuardianRequest> guardians = List.of(
                new PatientRequest.GuardianRequest("Maria Responsável", "52998224725"));

        PatientResponse response = service.create(request(PatientIdentityType.NEWBORN, null, true, guardians));

        assertThat(response.identityNumber()).isNull();
        assertThat(response.guardians()).singleElement()
                .satisfies(guardian -> assertThat(guardian.fullName()).isEqualTo("Maria Responsável"));
    }

    @Test
    void requiresHistoryEvidenceForPatientWithAppointments() {
        Patient patient = patient();
        when(repository.findForUpdateById(patient.getId())).thenReturn(Optional.of(patient));
        when(patientHistory.summarize(patient.getId())).thenReturn(new PatientHistoryEvidence(1, 0, List.of()));

        assertThatThrownBy(() -> service.inactivate(patient.getId(), new PatientInactivationRequest(true, false)))
                .isInstanceOf(PatientDomainException.class)
                .hasMessageContaining("histórico exibido");
    }

    @Test
    void inactivatesPatientWithoutHistoryAfterConfirmation() {
        Patient patient = patient();
        when(repository.findForUpdateById(patient.getId())).thenReturn(Optional.of(patient));
        when(patientHistory.summarize(patient.getId())).thenReturn(new PatientHistoryEvidence(0, 0, List.of()));

        PatientResponse response = service.inactivate(patient.getId(), new PatientInactivationRequest(true, false));

        assertThat(response.active()).isFalse();
    }

    private Patient patient() {
        Patient patient = new Patient(UUID.randomUUID(), "Paciente", PatientIdentityType.CPF, "52998224725",
                LocalDate.of(1990, 5, 20), "11999999999", "Nenhuma", true);
        patient.replaceAddress(new PatientAddress("01001000", "Praça da Sé", "1", null,
                "Sé", "São Paulo", "SP", "3550308"));
        return patient;
    }

    private PatientRequest request(PatientIdentityType type, String identity, boolean allergiesConfirmed,
                                   List<PatientRequest.GuardianRequest> guardians) {
        return new PatientRequest("  Paciente Teste  ", type, identity, LocalDate.of(1990, 5, 20),
                " 11999999999 ", "Nenhuma", allergiesConfirmed,
                new PatientRequest.AddressRequest("01001000", "Praça da Sé", "1", null,
                        "Sé", "São Paulo", "sp", "3550308"), guardians);
    }
}
