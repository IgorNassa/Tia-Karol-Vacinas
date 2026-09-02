package br.com.tiakarol.api.security;

import br.com.tiakarol.api.audit.AuditService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService {
    private final ApplicationUserRepository repository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    UserService(ApplicationUserRepository repository, AuthSessionRepository sessionRepository,
                PasswordEncoder passwordEncoder, AuditService auditService) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    UserResponse create(CreateUserRequest request) {
        UserRequest userRequest = request.user();
        validateRole(userRequest.role());
        String email = normalizeEmail(userRequest.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new SecurityDomainException("Já existe usuário com esse e-mail.");
        }
        ApplicationUser user = repository.save(new ApplicationUser(userRequest.fullName().trim(), email,
                passwordEncoder.encode(request.password()), userRequest.role()));
        if (!userRequest.active()) {
            user.update(user.getFullName(), user.getEmail(), user.getRole(), false);
        }
        UserResponse response = response(user);
        auditService.log("APP_USER", user.getId(), "USER_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    Page<UserResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    UserResponse get(UUID id) {
        return response(find(id));
    }

    @Transactional
    UserResponse update(UUID id, UserRequest request) {
        validateRole(request.role());
        ApplicationUser user = find(id);
        UserResponse before = response(user);
        String email = normalizeEmail(request.email());
        repository.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new SecurityDomainException("Já existe usuário com esse e-mail."); });
        protectLastAdmin(user, request.role(), request.active());
        user.update(request.fullName().trim(), email, request.role(), request.active());
        if (!request.active()) {
            sessionRepository.revokeAllByUserId(id);
        }
        UserResponse after = response(user);
        auditService.log("APP_USER", id, "USER_UPDATED", before, after, null);
        return after;
    }

    @Transactional
    void resetPassword(UUID id, String password) {
        ApplicationUser user = find(id);
        user.changePassword(passwordEncoder.encode(password));
        sessionRepository.revokeAllByUserId(id);
        auditService.log("APP_USER", id, "USER_PASSWORD_RESET", null, null,
                "Todas as sessões do usuário foram revogadas.");
    }

    private void protectLastAdmin(ApplicationUser user, UserRole newRole, boolean active) {
        if (user.getRole() == UserRole.ADMIN && user.isActive()
                && (newRole != UserRole.ADMIN || !active)
                && repository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new SecurityDomainException("Não é possível inativar ou rebaixar o último administrador ativo.");
        }
    }

    private void validateRole(UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.ATTENDANT) {
            throw new SecurityDomainException("Somente os papéis ADMIN e ATTENDANT estão habilitados nesta versão.");
        }
    }

    private ApplicationUser find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new SecurityDomainException("Usuário não encontrado."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse response(ApplicationUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
