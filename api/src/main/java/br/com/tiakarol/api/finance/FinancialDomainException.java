package br.com.tiakarol.api.finance;

public class FinancialDomainException extends RuntimeException {
    public FinancialDomainException(String message) {
        super(message);
    }
}
