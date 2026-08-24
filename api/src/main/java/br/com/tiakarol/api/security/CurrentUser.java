package br.com.tiakarol.api.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public UUID id() {
        return authenticatedUser().id();
    }

    public UserRole role() {
        String authority = authenticatedUser().authorities().stream()
                .map(granted -> granted.getAuthority())
                .filter(value -> value.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Perfil do usuário autenticado não encontrado."));
        return UserRole.valueOf(authority.substring("ROLE_".length()));
    }

    private AuthenticatedUser authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Usuário autenticado não encontrado.");
        }
        return user;
    }
}
