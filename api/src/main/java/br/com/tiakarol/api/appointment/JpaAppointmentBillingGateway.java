package br.com.tiakarol.api.appointment;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaAppointmentBillingGateway implements AppointmentBillingGateway {
    private final AppointmentRepository repository;

    JpaAppointmentBillingGateway(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public BillingDetails getBilling(UUID appointmentId) {
        return details(repository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentDomainException("Agendamento não encontrado.")));
    }

    @Override
    public BillingDetails lockForPayment(UUID appointmentId) {
        Appointment appointment = locked(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new AppointmentDomainException("Agendamento cancelado ou com falta não pode receber pagamento.");
        }
        return details(appointment);
    }

    @Override
    public BillingDetails lockForUpdate(UUID appointmentId) {
        return details(locked(appointmentId));
    }

    private Appointment locked(UUID appointmentId) {
        return repository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentDomainException("Agendamento não encontrado."));
    }

    private BillingDetails details(Appointment appointment) {
        return new BillingDetails(appointment.getId(), appointment.getFinalAmount());
    }
}
