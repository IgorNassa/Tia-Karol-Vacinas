package br.com.tiakarol.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private ApplicationUserRepository userRepository;
    @Mock private AuthSessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenCodec tokenCodec;
    @Mock private AuditService auditService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, sessionRepository, passwordEncoder, tokenCodec, auditService, 15, 7);
    }

    @Test
    void logsInActiveUserAndPersistsOnlyTokenHashes() {
        ApplicationUser user = user();
        when(userRepository.findByEmailIgnoreCase("admin@tia.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded")).thenReturn(true);
        when(tokenCodec.generate()).thenReturn("access-token", "refresh-token");
        when(tokenCodec.hash("access-token")).thenReturn("access-hash");
        when(tokenCodec.hash("refresh-token")).thenReturn("refresh-hash");

        TokenResponse response = service.login(new LoginRequest("ADMIN@TIA.COM", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(sessionRepository).save(any(AuthSession.class));
        verify(auditService).logAs(eq(user.getId()), eq("AUTH_SESSION"), eq(user.getId()),
                eq("USER_LOGGED_IN"), any(), any(), any());
    }

    @Test
    void rejectsInvalidPasswordWithoutCreatingSession() {
        ApplicationUser user = user();
        when(userRepository.findByEmailIgnoreCase("admin@tia.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin@tia.com", "wrong")))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void rotatesRefreshToken() {
        ApplicationUser user = user();
        AuthSession previous = new AuthSession(user.getId(), "old-access", "old-refresh",
                OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().plusDays(1));
        when(tokenCodec.hash("refresh-token")).thenReturn("old-refresh");
        when(sessionRepository.findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(
                eq("old-refresh"), any())).thenReturn(Optional.of(previous));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenCodec.generate()).thenReturn("new-access", "new-refresh");
        when(tokenCodec.hash("new-access")).thenReturn("new-access-hash");
        when(tokenCodec.hash("new-refresh")).thenReturn("new-refresh-hash");

        TokenResponse response = service.refresh(new RefreshTokenRequest("refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        verify(sessionRepository).save(any(AuthSession.class));
    }

    private ApplicationUser user() {
        return new ApplicationUser("Administrador", "admin@tia.com", "encoded", UserRole.ADMIN);
    }
}
