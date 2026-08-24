package br.com.tiakarol.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private ApplicationUserRepository repository;
    @Mock private AuthSessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, sessionRepository, passwordEncoder, auditService);
    }

    @Test
    void createsEnabledAttendantWithNormalizedEmail() {
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded");
        when(repository.save(any(ApplicationUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.create(new CreateUserRequest(
                new UserRequest("Atendente", " ATENDENTE@TIA.COM ", UserRole.ATTENDANT, true),
                "strong-password"));

        assertThat(response.email()).isEqualTo("atendente@tia.com");
        assertThat(response.role()).isEqualTo(UserRole.ATTENDANT);
        assertThat(response.active()).isTrue();
    }

    @Test
    void rejectsRoleThatIsNotEnabledYet() {
        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                new UserRequest("Financeiro", "financeiro@tia.com", UserRole.FINANCIAL, true),
                "strong-password")))
                .isInstanceOf(SecurityDomainException.class)
                .hasMessage("Somente os papéis ADMIN e ATTENDANT estão habilitados nesta versão.");

        verify(repository, never()).save(any());
    }

    @Test
    void protectsLastActiveAdministrator() {
        ApplicationUser admin = new ApplicationUser("Admin", "admin@tia.com", "encoded", UserRole.ADMIN);
        when(repository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(repository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(admin.getId(),
                new UserRequest("Admin", "admin@tia.com", UserRole.ATTENDANT, true)))
                .isInstanceOf(SecurityDomainException.class)
                .hasMessage("Não é possível inativar ou rebaixar o último administrador ativo.");
    }

    @Test
    void passwordResetRevokesEverySession() {
        ApplicationUser attendant = new ApplicationUser("Atendente", "atendente@tia.com", "old", UserRole.ATTENDANT);
        UUID id = attendant.getId();
        when(repository.findById(id)).thenReturn(Optional.of(attendant));
        when(passwordEncoder.encode("new-strong-password")).thenReturn("new-hash");

        service.resetPassword(id, "new-strong-password");

        verify(sessionRepository).revokeAllByUserId(id);
        verify(auditService).log("APP_USER", id, "USER_PASSWORD_RESET", null, null,
                "Todas as sessões do usuário foram revogadas.");
    }
}
