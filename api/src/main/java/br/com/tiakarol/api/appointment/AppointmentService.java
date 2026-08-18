package br.com.tiakarol.api.appointment;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.patient.PatientStatusGateway;
import br.com.tiakarol.api.security.CurrentUser;
import br.com.tiakarol.api.stock.VaccineLotInventory;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AppointmentService {
    private static final int DOSE_QUANTITY = 1;

    private final AppointmentRepository repository;
    private final PatientStatusGateway patientStatus;
    private final VaccineLotInventory inventory;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    AppointmentService(AppointmentRepository repository, PatientStatusGateway patientStatus,
                       VaccineLotInventory inventory, CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.patientStatus = patientStatus;
        this.inventory = inventory;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional
    AppointmentResponse create(CreateAppointmentRequest request) {
        if (!request.scheduledAt().isAfter(OffsetDateTime.now())) {
            throw new AppointmentDomainException("O agendamento precisa estar no futuro.");
        }
        if (request.discountAmount().compareTo(request.grossAmount()) > 0) {
            throw new AppointmentDomainException("O desconto não pode exceder o valor bruto.");
        }
        patientStatus.requireActive(request.patientId());
        UUID appointmentId = UUID.randomUUID();
        inventory.reserve(request.vaccineLotId(), DOSE_QUANTITY, appointmentId);
        Appointment appointment = repository.save(new Appointment(appointmentId, request, currentUser.id()));
        AppointmentResponse response = toResponse(appointment);
        auditService.log("APPOINTMENT", appointmentId, "APPOINTMENT_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    AppointmentResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    Page<AppointmentResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    Page<AppointmentResponse> pending(Pageable pageable) {
        return repository.findByStatusAndReservationStatus(AppointmentStatus.NO_SHOW,
                ReservationStatus.PENDING_DECISION, pageable).map(this::toResponse);
    }

    @Transactional
    AppointmentResponse confirm(UUID id) {
        Appointment appointment = find(id);
        AppointmentResponse before = toResponse(appointment);
        appointment.confirm();
        return audit(appointment, "APPOINTMENT_CONFIRMED", before, null);
    }

    @Transactional
    AppointmentResponse apply(UUID id, ApplyAppointmentRequest request) {
        Appointment appointment = find(id);
        AppointmentResponse before = toResponse(appointment);
        appointment.apply(request);
        inventory.apply(appointment.getVaccineLotId(), DOSE_QUANTITY, appointment.getId());
        return audit(appointment, "APPOINTMENT_APPLIED", before, null);
    }

    @Transactional
    AppointmentResponse cancel(UUID id, String reason) {
        requireReason(reason);
        Appointment appointment = find(id);
        AppointmentResponse before = toResponse(appointment);
        appointment.cancel(reason);
        inventory.release(appointment.getVaccineLotId(), DOSE_QUANTITY, appointment.getId(), reason);
        return audit(appointment, "APPOINTMENT_CANCELLED", before, reason.trim());
    }

    @Transactional
    AppointmentResponse markNoShow(UUID id) {
        Appointment appointment = find(id);
        AppointmentResponse before = toResponse(appointment);
        appointment.markNoShow();
        return audit(appointment, "APPOINTMENT_NO_SHOW", before, "Aguardando decisão de estoque.");
    }

    @Transactional
    AppointmentResponse resolveNoShow(UUID id, ResolveNoShowRequest request) {
        Appointment appointment = find(id);
        AppointmentResponse before = toResponse(appointment);
        appointment.resolveNoShow(request.resolution(), request.reason());
        if (request.resolution() == NoShowResolution.RETURN_TO_STOCK) {
            inventory.release(appointment.getVaccineLotId(), DOSE_QUANTITY, appointment.getId(), request.reason());
        }
        return audit(appointment, "APPOINTMENT_NO_SHOW_RESOLVED", before, request.reason().trim());
    }

    private Appointment find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppointmentDomainException("Agendamento não encontrado."));
    }

    private AppointmentResponse audit(Appointment appointment, String action, AppointmentResponse before,
                                      String reason) {
        AppointmentResponse response = toResponse(appointment);
        auditService.log("APPOINTMENT", appointment.getId(), action, before, response, reason);
        return response;
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getPatientId(),
                appointment.getVaccineLotId(), appointment.getScheduledAt(), appointment.getStatus(),
                appointment.getReservationStatus(), appointment.getApplicationLocation(), appointment.getReactions(),
                appointment.getNotes(), appointment.getCancellationReason(),
                appointment.getReservationResolutionReason(), appointment.getGrossAmount(),
                appointment.getDiscountAmount(), appointment.getFinalAmount(), appointment.getAppliedAt());
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppointmentDomainException("O motivo é obrigatório.");
        }
    }
}
