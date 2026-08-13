package br.com.tiakarol.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class BootstrapAdminConfiguration {
    @Bean
    CommandLineRunner createBootstrapAdmin(ApplicationUserRepository repository, PasswordEncoder passwordEncoder,
                                           @Value("${bootstrap.admin.email}") String email,
                                           @Value("${bootstrap.admin.password}") String password,
                                           @Value("${bootstrap.admin.name}") String name) {
        return arguments -> {
            if (!email.isBlank() && !password.isBlank() && repository.findByEmailIgnoreCase(email).isEmpty()) {
                repository.save(new ApplicationUser(name, email.trim().toLowerCase(),
                        passwordEncoder.encode(password), UserRole.ADMIN));
            }
        };
    }
}
