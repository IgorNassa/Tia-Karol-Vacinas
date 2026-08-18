package br.com.tiakarol.api.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.patient.PatientStatusGateway;
import br.com.tiakarol.api.security.CurrentUser;
import br.com.tiakarol.api.stock.VaccineLotInventory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LOT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private AppointmentRepository repository;
    @Mock
    private PatientStatusGateway patientStatus;
    @Mock
    private VaccineLotInventory inventory;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditService auditService;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(repository, patientStatus, inventory, currentUser, auditService);
    }

    @Test
    void createsAppointmentAndReservesExactlyOneDose() {
        when(currentUser.id()).thenReturn(USER_ID);
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = service.create(request("100.00", "15.00"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(response.finalAmount()).isEqualByComparingTo("85.00");
        verify(patientStatus).requireActive(PATIENT_ID);
        verify(inventory).reserve(LOT_ID, 1, response.id());
        verify(auditService).log("APPOINTMENT", response.id(), "APPOINTMENT_CREATED", null, response, null);
    }

    @Test
    void rejectsDiscountGreaterThanGrossAmountWithoutReservingStock() {
        assertThatThrownBy(() -> service.create(request("50.00", "60.00")))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessage("O desconto não pode exceder o valor bruto.");

        verify(patientStatus, never()).requireActive(any());
        verify(inventory, never()).reserve(any(), any(Integer.class), any());
    }

    @Test
    void appliesAppointmentAndConsumesReservedDose() {
        Appointment appointment = appointment();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.apply(appointment.getId(),
                new ApplyAppointmentRequest("Clínica", "Sem reações", "Aplicada no braço esquerdo"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.APPLIED);
        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.CONSUMED);
        assertThat(response.appliedAt()).isNotNull();
        verify(inventory).apply(LOT_ID, 1, appointment.getId());
    }

    @Test
    void cancelsActiveAppointmentWithReasonAndReleasesDose() {
        Appointment appointment = appointment();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.cancel(appointment.getId(), "Paciente solicitou cancelamento");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(inventory).release(LOT_ID, 1, appointment.getId(), "Paciente solicitou cancelamento");
    }

    @Test
    void requiresReasonBeforeCancellation() {
        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), " "))
                .isInstanceOf(AppointmentDomainException.class)
                .hasMessage("O motivo é obrigatório.");

        verify(repository, never()).findById(any());
    }

    @Test
    void keepsNoShowDoseReservedUntilAdminDecision() {
        Appointment appointment = appointment();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.markNoShow(appointment.getId());

        assertThat(response.status()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.PENDING_DECISION);
        verify(inventory, never()).release(any(), any(Integer.class), any(), any());
    }

    @Test
    void returnsNoShowDoseToStockWhenAdminChoosesReturn() {
        Appointment appointment = appointment();
        appointment.markNoShow();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.resolveNoShow(appointment.getId(),
                new ResolveNoShowRequest(NoShowResolution.RETURN_TO_STOCK, "Paciente não reagendou"));

        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(inventory).release(LOT_ID, 1, appointment.getId(), "Paciente não reagendou");
    }

    @Test
    void keepsNoShowReservationWithoutCreatingStockMovement() {
        Appointment appointment = appointment();
        appointment.markNoShow();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.resolveNoShow(appointment.getId(),
                new ResolveNoShowRequest(NoShowResolution.KEEP_RESERVED, "Reagendamento em negociação"));

        assertThat(response.reservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        verify(inventory, never()).release(any(), any(Integer.class), any(), any());
    }

    private Appointment appointment() {
        return new Appointment(UUID.randomUUID(), request("100.00", "0.00"), USER_ID);
    }

    private CreateAppointmentRequest request(String gross, String discount) {
        return new CreateAppointmentRequest(PATIENT_ID, LOT_ID, OffsetDateTime.now().plusDays(2),
                new BigDecimal(gross), new BigDecimal(discount), "Primeira dose");
    }
}
