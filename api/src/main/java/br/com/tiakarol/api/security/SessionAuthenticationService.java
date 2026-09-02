package br.com.tiakarol.api.security;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SessionAuthenticationService {
    private final AuthSessionRepository sessionRepository;
    private final ApplicationUserRepository userRepository;
    private final TokenCodec tokenCodec;

    SessionAuthenticationService(AuthSessionRepository sessionRepository,
                                 ApplicationUserRepository userRepository, TokenCodec tokenCodec) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.tokenCodec = tokenCodec;
    }

    @Transactional
    Optional<AuthenticatedUser> authenticate(String accessToken) {
        return sessionRepository.findByAccessTokenHashAndRevokedAtIsNullAndAccessExpiresAtAfter(
                        tokenCodec.hash(accessToken), OffsetDateTime.now())
                .flatMap(session -> userRepository.findById(session.getUserId())
                        .filter(ApplicationUser::isActive)
                        .map(user -> {
                            session.markUsed();
                            return new AuthenticatedUser(user.getId(), user.getEmail(), "", true,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                        }));
    }
}
