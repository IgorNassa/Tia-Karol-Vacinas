package br.com.tiakarol.api.payment;

public class PaymentDomainException extends RuntimeException {
    public PaymentDomainException(String message) {
        super(message);
    }
}
