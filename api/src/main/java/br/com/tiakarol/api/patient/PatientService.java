package br.com.tiakarol.api.patient;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.appointment.PatientHistoryEvidence;
import br.com.tiakarol.api.appointment.PatientHistoryGateway;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PatientService {
    private final PatientRepository repository;
    private final AuditService auditService;
    private final PatientHistoryGateway patientHistory;

    PatientService(PatientRepository repository, AuditService auditService, PatientHistoryGateway patientHistory) {
        this.repository = repository;
        this.auditService = auditService;
        this.patientHistory = patientHistory;
    }

    @Transactional
    PatientResponse create(PatientRequest request) {
        validate(request, null);
        Patient patient = new Patient(UUID.randomUUID(), request.fullName().trim(), request.identityType(),
                normalizeIdentity(request.identityNumber()), request.birthDate(), request.phone().trim(),
                request.allergies().trim(), request.allergiesConfirmed());
        applyDetails(patient, request);
        PatientResponse response = toResponse(repository.save(patient));
        auditService.log("PATIENT", patient.getId(), "PATIENT_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    PatientResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    Page<PatientResponse> list(String search, Pageable pageable) {
        return (search == null || search.isBlank() ? repository.findAll(pageable)
                : repository.findByFullNameContainingIgnoreCase(search.trim(), pageable)).map(this::toResponse);
    }

    @Transactional
    PatientResponse update(UUID id, PatientRequest request) {
        Patient patient = find(id);
        PatientResponse before = toResponse(patient);
        validate(request, patient.getId());
        patient.update(request.fullName().trim(), request.identityType(), normalizeIdentity(request.identityNumber()),
                request.birthDate(), request.phone().trim(), request.allergies().trim(), request.allergiesConfirmed());
        applyDetails(patient, request);
        PatientResponse response = toResponse(patient);
        auditService.log("PATIENT", patient.getId(), "PATIENT_UPDATED", before, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    PatientInactivationPreview previewInactivation(UUID id) {
        Patient patient = find(id);
        PatientHistoryEvidence evidence = patientHistory.summarize(id);
        return new PatientInactivationPreview(id, patient.getFullName(), evidence.hasHistory(), evidence);
    }

    @Transactional
    PatientResponse inactivate(UUID id, PatientInactivationRequest request) {
        if (!request.confirmationAccepted()) {
            throw new PatientDomainException("A confirmação da inativação é obrigatória.");
        }
        Patient patient = find(id);
        PatientHistoryEvidence evidence = patientHistory.summarize(id);
        if (evidence.hasHistory() && !request.historyEvidenceAccepted()) {
            throw new PatientDomainException("Confirme também que o histórico exibido foi revisado.");
        }
        PatientResponse before = toResponse(patient);
        if (patient.isActive()) {
            patient.inactivate();
        }
        PatientResponse response = toResponse(patient);
        auditService.log("PATIENT", patient.getId(), "PATIENT_INACTIVATED", before, response,
                evidence.hasHistory() ? "Histórico revisado e confirmação dupla aceita." : "Confirmação aceita.");
        return response;
    }

    @Transactional
    void delete(UUID id) {
        Patient patient = find(id);
        if (patient.isActive() || patient.getInactivatedAt() == null
                || patient.getInactivatedAt().isAfter(OffsetDateTime.now().minusMonths(3))) {
            throw new PatientDomainException("O paciente só pode ser excluído após três meses de inativação.");
        }
        auditService.log("PATIENT", patient.getId(), "PATIENT_DELETED", toResponse(patient), null,
                "Inativo há pelo menos três meses.");
        repository.delete(patient);
    }

    private Patient find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PatientDomainException("Paciente não encontrado."));
    }

    private void validate(PatientRequest request, UUID currentId) {
        if (!request.allergiesConfirmed()) {
            throw new PatientDomainException("A confirmação das informações de alergia é obrigatória.");
        }
        if (request.identityType() == PatientIdentityType.NEWBORN) {
            if (request.guardians() == null || request.guardians().isEmpty()) {
                throw new PatientDomainException("Recém-nascido precisa de ao menos um responsável.");
            }
            return;
        }
        String identityNumber = normalizeIdentity(request.identityNumber());
        if (identityNumber == null) {
            throw new PatientDomainException("Documento de identificação é obrigatório.");
        }
        if (request.identityType() == PatientIdentityType.CPF && !isValidCpf(identityNumber)) {
            throw new PatientDomainException("CPF inválido.");
        }
        boolean exists = request.identityType() == PatientIdentityType.CPF
                && repository.existsByIdentityTypeAndIdentityNumber(request.identityType(), identityNumber);
        if (exists && (currentId == null || repository.findById(currentId)
                .map(patient -> !identityNumber.equals(patient.getIdentityNumber())
                        || request.identityType() != patient.getIdentityType())
                .orElse(true))) {
            throw new PatientDomainException("Já existe paciente com esse documento.");
        }
    }

    private void applyDetails(Patient patient, PatientRequest request) {
        PatientRequest.AddressRequest address = request.address();
        patient.replaceAddress(new PatientAddress(address.postalCode().trim(), address.street().trim(),
                address.number().trim(), trimToNull(address.complement()), address.district().trim(),
                address.city().trim(), address.state().trim().toUpperCase(Locale.ROOT), trimToNull(address.ibgeCode())));
        List<PatientGuardian> guardians = request.guardians() == null ? List.of() : request.guardians().stream()
                .map(guardian -> new PatientGuardian(guardian.fullName().trim(), guardian.identityNumber().trim()))
                .toList();
        patient.replaceGuardians(guardians);
    }

    private PatientResponse toResponse(Patient patient) {
        PatientAddress address = patient.getAddress();
        PatientResponse.AddressResponse addressResponse = new PatientResponse.AddressResponse(address.getPostalCode(),
                address.getStreet(), address.getNumber(), address.getComplement(), address.getDistrict(),
                address.getCity(), address.getState(), address.getIbgeCode());
        List<PatientResponse.GuardianResponse> guardians = patient.getGuardians().stream()
                .map(guardian -> new PatientResponse.GuardianResponse(guardian.getFullName(), guardian.getIdentityNumber()))
                .toList();
        return new PatientResponse(patient.getId(), patient.getFullName(), patient.getIdentityType(),
                patient.getIdentityNumber(), patient.getBirthDate(), patient.getPhone(), patient.getAllergies(),
                patient.isAllergiesConfirmed(), patient.isActive(), patient.getInactivatedAt(), addressResponse, guardians);
    }

    private String normalizeIdentity(String value) {
        return trimToNull(value == null ? null : value.replaceAll("[^A-Za-z0-9]", ""));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isValidCpf(String cpf) {
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return cpf.charAt(9) - '0' == cpfDigit(cpf, 9) && cpf.charAt(10) - '0' == cpfDigit(cpf, 10);
    }

    private int cpfDigit(String cpf, int position) {
        int sum = 0;
        for (int index = 0; index < position; index++) {
            sum += (cpf.charAt(index) - '0') * (position + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
