package br.com.tiakarol.api.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class DatabaseUserDetailsService implements UserDetailsService {
    private final ApplicationUserRepository repository;

    DatabaseUserDetailsService(ApplicationUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        ApplicationUser user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.isActive(),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
