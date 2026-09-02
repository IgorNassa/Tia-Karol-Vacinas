package br.com.tiakarol.api.payment;

import br.com.tiakarol.api.appointment.AppointmentPaymentStateGateway;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaAppointmentPaymentStateGateway implements AppointmentPaymentStateGateway {
    private final PaymentEntryRepository repository;

    JpaAppointmentPaymentStateGateway(PaymentEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void requireNoActivePayment(UUID appointmentId, String operation) {
        if (repository.existsByAppointmentIdAndActiveTrue(appointmentId)) {
            throw new PaymentDomainException("Estorne o pagamento ativo antes de " + operation + ".");
        }
    }
}
