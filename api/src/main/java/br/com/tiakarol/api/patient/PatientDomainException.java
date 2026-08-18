package br.com.tiakarol.api.patient;

public class PatientDomainException extends RuntimeException {
    public PatientDomainException(String message) {
        super(message);
    }
}
