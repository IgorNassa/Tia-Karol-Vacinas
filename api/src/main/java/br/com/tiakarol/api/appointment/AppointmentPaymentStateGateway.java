package br.com.tiakarol.api.appointment;

import java.util.UUID;

public interface AppointmentPaymentStateGateway {
    void requireNoActivePayment(UUID appointmentId, String operation);
}
