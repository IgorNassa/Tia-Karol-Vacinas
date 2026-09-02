package br.com.tiakarol.api.appointment;

import java.math.BigDecimal;
import java.util.UUID;

public interface AppointmentBillingGateway {
    BillingDetails getBilling(UUID appointmentId);

    BillingDetails lockForPayment(UUID appointmentId);

    BillingDetails lockForUpdate(UUID appointmentId);

    record BillingDetails(UUID appointmentId, BigDecimal expectedAmount) { }
}
