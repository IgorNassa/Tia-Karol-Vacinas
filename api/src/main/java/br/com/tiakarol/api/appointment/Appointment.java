package br.com.tiakarol.api.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
class Appointment {
    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "vaccine_lot_id", nullable = false)
    private UUID vaccineLotId;
    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private ReservationStatus reservationStatus;
    @Column(name = "application_location")
    private String applicationLocation;
    private String reactions;
    private String notes;
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    @Column(name = "reservation_resolution_reason")
    private String reservationResolutionReason;
    @Column(name = "rescheduled_at")
    private OffsetDateTime rescheduledAt;
    @Column(name = "reschedule_reason")
    private String rescheduleReason;
    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;
    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;
    @Column(name = "final_amount", nullable = false)
    private BigDecimal finalAmount;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Appointment() { }

    Appointment(UUID id, CreateAppointmentRequest request, UUID createdBy) {
        this.id = id;
        this.patientId = request.patientId();
        this.vaccineLotId = request.vaccineLotId();
        this.scheduledAt = request.scheduledAt();
        this.status = AppointmentStatus.SCHEDULED;
        this.reservationStatus = ReservationStatus.RESERVED;
        this.notes = trimToNull(request.notes());
        this.grossAmount = request.grossAmount();
        this.discountAmount = request.discountAmount();
        this.finalAmount = request.grossAmount().subtract(request.discountAmount());
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    UUID getId() { return id; }
    UUID getPatientId() { return patientId; }
    UUID getVaccineLotId() { return vaccineLotId; }
    OffsetDateTime getScheduledAt() { return scheduledAt; }
    AppointmentStatus getStatus() { return status; }
    ReservationStatus getReservationStatus() { return reservationStatus; }
    String getApplicationLocation() { return applicationLocation; }
    String getReactions() { return reactions; }
    String getNotes() { return notes; }
    String getCancellationReason() { return cancellationReason; }
    String getReservationResolutionReason() { return reservationResolutionReason; }
    BigDecimal getGrossAmount() { return grossAmount; }
    BigDecimal getDiscountAmount() { return discountAmount; }
    BigDecimal getFinalAmount() { return finalAmount; }
    OffsetDateTime getAppliedAt() { return appliedAt; }
    OffsetDateTime getRescheduledAt() { return rescheduledAt; }
    String getRescheduleReason() { return rescheduleReason; }

    void confirm() {
        requireStatus(AppointmentStatus.SCHEDULED, "Somente agendamento pendente pode ser confirmado.");
        status = AppointmentStatus.CONFIRMED;
        touch();
    }

    void apply(ApplyAppointmentRequest request) {
        if (status != AppointmentStatus.SCHEDULED && status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentDomainException("Somente agendamento ativo pode registrar aplicação.");
        }
        if (reservationStatus != ReservationStatus.RESERVED) {
            throw new AppointmentDomainException("O agendamento não possui uma dose reservada.");
        }
        status = AppointmentStatus.APPLIED;
        reservationStatus = ReservationStatus.CONSUMED;
        applicationLocation = request.applicationLocation().trim();
        reactions = trimToNull(request.reactions());
        notes = trimToNull(request.notes());
        appliedAt = OffsetDateTime.now();
        touch();
    }

    void cancel(String reason) {
        if (status != AppointmentStatus.SCHEDULED && status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentDomainException("Somente agendamento ativo pode ser cancelado.");
        }
        status = AppointmentStatus.CANCELLED;
        reservationStatus = ReservationStatus.RELEASED;
        cancellationReason = reason.trim();
        touch();
    }

    void markNoShow() {
        if (status != AppointmentStatus.SCHEDULED && status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentDomainException("Somente agendamento ativo pode ser marcado como falta.");
        }
        status = AppointmentStatus.NO_SHOW;
        reservationStatus = ReservationStatus.PENDING_DECISION;
        touch();
    }

    void resolveNoShow(NoShowResolution resolution, String reason) {
        if (status != AppointmentStatus.NO_SHOW || reservationStatus != ReservationStatus.PENDING_DECISION) {
            throw new AppointmentDomainException("O agendamento não possui pendência de estoque.");
        }
        reservationStatus = resolution == NoShowResolution.RETURN_TO_STOCK
                ? ReservationStatus.RELEASED : ReservationStatus.RESERVED;
        reservationResolutionReason = reason.trim();
        touch();
    }

    void updateDetails(UpdateAppointmentRequest request, boolean administrator) {
        if (!administrator && request.patientId() != null) {
            throw new AppointmentDomainException("Somente administrador pode alterar o paciente.");
        }
        if (!administrator && (request.applicationLocation() != null || request.reactions() != null)) {
            throw new AppointmentDomainException("Somente administrador pode alterar dados da aplicação.");
        }
        if (request.patientId() != null) {
            if (!isActive()) throw new AppointmentDomainException("Paciente só pode ser alterado em agendamento ativo.");
            patientId = request.patientId();
        }
        BigDecimal newGross = request.grossAmount() == null ? grossAmount : request.grossAmount();
        BigDecimal newDiscount = request.discountAmount() == null ? discountAmount : request.discountAmount();
        if (newDiscount.compareTo(newGross) > 0) {
            throw new AppointmentDomainException("O desconto não pode exceder o valor bruto.");
        }
        grossAmount = newGross;
        discountAmount = newDiscount;
        finalAmount = newGross.subtract(newDiscount);
        if (request.notes() != null) notes = trimToNull(request.notes());
        if (request.applicationLocation() != null || request.reactions() != null) {
            if (status != AppointmentStatus.APPLIED) {
                throw new AppointmentDomainException("Dados da aplicação só podem ser alterados após a aplicação.");
            }
            if (request.applicationLocation() != null) applicationLocation = trimToNull(request.applicationLocation());
            if (request.reactions() != null) reactions = trimToNull(request.reactions());
        }
        touch();
    }

    void reschedule(OffsetDateTime newDate, UUID newLotId, String reason) {
        boolean activeAppointment = isActive();
        boolean retainedNoShow = status == AppointmentStatus.NO_SHOW && reservationStatus == ReservationStatus.RESERVED;
        if (!activeAppointment && !retainedNoShow) {
            throw new AppointmentDomainException("Somente agendamento ativo ou falta com dose mantida pode ser reagendado.");
        }
        scheduledAt = newDate;
        vaccineLotId = newLotId;
        status = AppointmentStatus.SCHEDULED;
        reservationStatus = ReservationStatus.RESERVED;
        rescheduledAt = OffsetDateTime.now();
        rescheduleReason = reason.trim();
        touch();
    }

    private boolean isActive() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;
    }

    private void requireStatus(AppointmentStatus expected, String message) {
        if (status != expected) {
            throw new AppointmentDomainException(message);
        }
    }

    private void touch() {
        updatedAt = OffsetDateTime.now();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
