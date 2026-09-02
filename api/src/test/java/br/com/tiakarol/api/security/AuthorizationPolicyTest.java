package br.com.tiakarol.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuthorizationPolicyTest {
    private final AuthorizationPolicy policy = new AuthorizationPolicy(true);

    @ParameterizedTest(name = "{0} {1} - {2}")
    @MethodSource("matrix")
    void enforcesCompleteRoleMatrix(String method, String path, UserRole role, boolean expected) {
        assertThat(policy.isAllowed(method, path, role != null,
                role == null ? Set.of() : Set.of(role))).isEqualTo(expected);
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                row("GET", "/api/v1/health", null, true),
                row("GET", "/actuator/health/readiness", null, true),
                row("POST", "/api/v1/auth/login", null, true),
                row("POST", "/api/v1/auth/refresh", null, true),
                row("GET", "/v3/api-docs", null, true),
                row("GET", "/api/v1/patients", null, false),

                row("GET", "/api/v1/users", UserRole.ADMIN, true),
                row("GET", "/api/v1/users", UserRole.ATTENDANT, false),
                row("GET", "/api/v1/financial/entries", UserRole.FINANCIAL, false),
                row("GET", "/api/v1/reports/stock", UserRole.ATTENDANT, false),

                row("POST", "/api/v1/patients", UserRole.ATTENDANT, true),
                row("DELETE", "/api/v1/patients/123", UserRole.ATTENDANT, false),
                row("PATCH", "/api/v1/patients/123/inactivation", UserRole.ATTENDANT, false),
                row("GET", "/api/v1/patients", UserRole.APPLICATOR, false),

                row("GET", "/api/v1/appointments", UserRole.ATTENDANT, true),
                row("POST", "/api/v1/appointments", UserRole.ATTENDANT, true),
                row("PATCH", "/api/v1/appointments/123/reschedule", UserRole.ATTENDANT, false),
                row("PATCH", "/api/v1/appointments/123/no-show-resolution", UserRole.ATTENDANT, false),
                row("GET", "/api/v1/appointments", UserRole.APPLICATOR, true),
                row("GET", "/api/v1/appointments/123", UserRole.APPLICATOR, true),
                row("PATCH", "/api/v1/appointments/123/application", UserRole.APPLICATOR, true),
                row("POST", "/api/v1/appointments", UserRole.APPLICATOR, false),
                row("PATCH", "/api/v1/appointments/123/cancellation", UserRole.APPLICATOR, false),
                row("PATCH", "/api/v1/appointments/123/reschedule", UserRole.APPLICATOR, false),

                row("GET", "/api/v1/appointments/123/payments", UserRole.ATTENDANT, true),
                row("PUT", "/api/v1/appointments/123/payments", UserRole.ATTENDANT, true),
                row("GET", "/api/v1/appointments/123/payments/history", UserRole.ATTENDANT, false),
                row("POST", "/api/v1/appointments/123/payments/void", UserRole.ATTENDANT, false),

                row("GET", "/api/v1/vaccines", UserRole.ATTENDANT, true),
                row("POST", "/api/v1/vaccines", UserRole.ATTENDANT, false),
                row("GET", "/api/v1/vaccine-lots/123", UserRole.ATTENDANT, true),
                row("POST", "/api/v1/vaccine-lots/123/stock-movements", UserRole.ATTENDANT, false),

                row("GET", "/api/v1/financial/entries", UserRole.ADMIN, true),
                row("GET", "/api/v1/reports/stock", UserRole.ADMIN, true),
                row("POST", "/api/v1/unknown", UserRole.ADMIN, true),
                row("POST", "/api/v1/auth/logout", UserRole.FINANCIAL, true),
                row("GET", "/api/v1/patients", UserRole.FINANCIAL, false)
        );
    }

    private static Arguments row(String method, String path, UserRole role, boolean expected) {
        return Arguments.of(method, path, role, expected);
    }

    @ParameterizedTest
    @MethodSource("privateOpenApiPaths")
    void protectsOpenApiWhenConfiguredForProduction(String path) {
        AuthorizationPolicy privatePolicy = new AuthorizationPolicy(false);
        assertThat(privatePolicy.isAllowed("GET", path, false, Set.of())).isFalse();
        assertThat(privatePolicy.isAllowed("GET", path, true, Set.of(UserRole.ADMIN))).isTrue();
    }

    static Stream<String> privateOpenApiPaths() {
        return Stream.of("/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui/index.html");
    }
}
