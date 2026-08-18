package br.com.tiakarol.api.appointment;

public class AppointmentDomainException extends RuntimeException {
    public AppointmentDomainException(String message) {
        super(message);
    }
}
