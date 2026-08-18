package br.com.tiakarol.api.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
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

    private PatientService service;

    @BeforeEach
    void setUp() {
        service = new PatientService(repository, auditService);
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

    private PatientRequest request(PatientIdentityType type, String identity, boolean allergiesConfirmed,
                                   List<PatientRequest.GuardianRequest> guardians) {
        return new PatientRequest("  Paciente Teste  ", type, identity, LocalDate.of(1990, 5, 20),
                " 11999999999 ", "Nenhuma", allergiesConfirmed,
                new PatientRequest.AddressRequest("01001000", "Praça da Sé", "1", null,
                        "Sé", "São Paulo", "sp", "3550308"), guardians);
    }
}
