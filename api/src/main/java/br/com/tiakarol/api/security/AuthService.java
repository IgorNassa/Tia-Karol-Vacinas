package br.com.tiakarol.api.security;

import br.com.tiakarol.api.audit.AuditService;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {
    private final ApplicationUserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCodec tokenCodec;
    private final AuditService auditService;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    AuthService(ApplicationUserRepository userRepository, AuthSessionRepository sessionRepository,
                PasswordEncoder passwordEncoder, TokenCodec tokenCodec, AuditService auditService,
                @Value("${auth.access-token-minutes}") long accessTokenMinutes,
                @Value("${auth.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCodec = tokenCodec;
        this.auditService = auditService;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    TokenResponse login(LoginRequest request) {
        ApplicationUser user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(ApplicationUser::isActive)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationDomainException("E-mail ou senha inválidos."));
        TokenResponse response = createSession(user);
        auditService.logAs(user.getId(), "AUTH_SESSION", user.getId(), "USER_LOGGED_IN", null, null, null);
        return response;
    }

    @Transactional
    TokenResponse refresh(RefreshTokenRequest request) {
        AuthSession previous = sessionRepository
                .findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(
                        tokenCodec.hash(request.refreshToken()), OffsetDateTime.now())
                .orElseThrow(() -> new AuthenticationDomainException("Refresh token inválido ou expirado."));
        ApplicationUser user = userRepository.findById(previous.getUserId())
                .filter(ApplicationUser::isActive)
                .orElseThrow(() -> new AuthenticationDomainException("Usuário inativo ou não encontrado."));
        previous.revoke();
        TokenResponse response = createSession(user);
        auditService.logAs(user.getId(), "AUTH_SESSION", previous.getId(), "SESSION_REFRESHED", null, null,
                "Refresh token rotacionado.");
        return response;
    }

    @Transactional
    void logout(String accessToken) {
        sessionRepository.findByAccessTokenHashAndRevokedAtIsNullAndAccessExpiresAtAfter(
                tokenCodec.hash(accessToken), OffsetDateTime.now()).ifPresent(session -> {
                    session.revoke();
                    auditService.logAs(session.getUserId(), "AUTH_SESSION", session.getId(), "USER_LOGGED_OUT",
                            null, null, null);
                });
    }

    private TokenResponse createSession(ApplicationUser user) {
        String accessToken = tokenCodec.generate();
        String refreshToken = tokenCodec.generate();
        OffsetDateTime accessExpiresAt = OffsetDateTime.now().plusMinutes(accessTokenMinutes);
        OffsetDateTime refreshExpiresAt = OffsetDateTime.now().plusDays(refreshTokenDays);
        sessionRepository.save(new AuthSession(user.getId(), tokenCodec.hash(accessToken),
                tokenCodec.hash(refreshToken), accessExpiresAt, refreshExpiresAt));
        return new TokenResponse("Bearer", accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
    }
}
