package br.com.tiakarol.api.security;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
class AuthorizationPolicy {
    private final AntPathMatcher paths = new AntPathMatcher();
    private final boolean publicOpenApi;

    AuthorizationPolicy(@Value("${app.openapi.public:true}") boolean publicOpenApi) {
        this.publicOpenApi = publicOpenApi;
    }

    AuthorizationDecision authorize(Supplier<Authentication> authentication,
                                    RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        boolean authenticated = current != null && current.isAuthenticated()
                && !(current instanceof AnonymousAuthenticationToken);
        Set<UserRole> roles = authenticated ? current.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .map(value -> UserRole.valueOf(value.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toUnmodifiableSet()) : Set.of();
        return new AuthorizationDecision(isAllowed(context.getRequest().getMethod(),
                context.getRequest().getRequestURI(), authenticated, roles));
    }

    boolean isAllowed(String method, String path, boolean authenticated, Set<UserRole> roles) {
        if (isPublic(method, path)) return true;
        if (!authenticated) return false;
        if (roles.contains(UserRole.ADMIN)) return true;

        if ("POST".equals(method) && "/api/v1/auth/logout".equals(path)) return true;
        if (isOpenApi(path)) return false;
        if (matches("/api/v1/patients/**", path)) {
            if ("DELETE".equals(method) || "PATCH".equals(method)
                    && matches("/api/v1/patients/*/inactivation", path)) return false;
            return roles.contains(UserRole.ATTENDANT);
        }
        if (matches("/api/v1/financial/**", path) || matches("/api/v1/reports/**", path)
                || matches("/api/v1/users/**", path)) return false;

        if (matches("/api/v1/appointments/*/payments/history", path)
                || matches("/api/v1/appointments/*/payments/void", path)) return false;
        if (matches("/api/v1/appointments/*/payments/**", path)) return roles.contains(UserRole.ATTENDANT);
        if (matches("/api/v1/appointments/**", path)) {
            if ("PATCH".equals(method) && (matches("/api/v1/appointments/*/no-show-resolution", path)
                    || matches("/api/v1/appointments/*/reschedule", path))) return false;
            if (roles.contains(UserRole.ATTENDANT)) return true;
            return roles.contains(UserRole.APPLICATOR)
                    && ("GET".equals(method) || "PATCH".equals(method)
                    && matches("/api/v1/appointments/*/application", path));
        }

        if (matches("/api/v1/vaccine-lots/**", path) || matches("/api/v1/vaccines/**", path)) {
            return "GET".equals(method) && roles.contains(UserRole.ATTENDANT);
        }
        return false;
    }

    private boolean isPublic(String method, String path) {
        if ("GET".equals(method) && ("/api/v1/health".equals(path)
                || matches("/actuator/health/**", path) || "/actuator/health".equals(path))) return true;
        if ("POST".equals(method) && ("/api/v1/auth/login".equals(path)
                || "/api/v1/auth/refresh".equals(path))) return true;
        return publicOpenApi && isOpenApi(path);
    }

    private boolean isOpenApi(String path) {
        return "/swagger-ui.html".equals(path) || matches("/swagger-ui/**", path)
                || matches("/v3/api-docs/**", path);
    }

    private boolean matches(String pattern, String path) {
        return paths.match(pattern, path);
    }
}
