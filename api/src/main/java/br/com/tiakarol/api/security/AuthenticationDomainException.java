package br.com.tiakarol.api.security;

public class AuthenticationDomainException extends SecurityDomainException {
    public AuthenticationDomainException(String message) {
        super(message);
    }
}
