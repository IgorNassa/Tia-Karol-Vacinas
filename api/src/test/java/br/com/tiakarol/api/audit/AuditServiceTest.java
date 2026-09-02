package br.com.tiakarol.api.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.tiakarol.api.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditServiceTest {
    private final AuditService service = new AuditService(mock(AuditLogRepository.class), mock(CurrentUser.class),
            new ObjectMapper().findAndRegisterModules());

    @Test
    void redactsSensitiveFieldsRecursivelyButKeepsOperationalEvidence() {
        Map<String, Object> sanitized = service.convert(Map.of(
                "id", "patient-id",
                "fullName", "Nome Real",
                "identityNumber", "52998224725",
                "address", Map.of("street", "Rua real", "state", "SP"),
                "guardians", List.of(Map.of("fullName", "Responsável", "identityNumber", "RG123")),
                "active", true));

        assertThat(sanitized).containsEntry("id", "patient-id").containsEntry("active", true)
                .containsEntry("fullName", "[REDACTED]").containsEntry("identityNumber", "[REDACTED]");
        Map<?, ?> address = (Map<?, ?>) sanitized.get("address");
        assertThat(address.get("street")).isEqualTo("[REDACTED]");
        assertThat(address.get("state")).isEqualTo("SP");
        Map<?, ?> guardian = (Map<?, ?>) ((List<?>) sanitized.get("guardians")).get(0);
        assertThat(guardian.get("fullName")).isEqualTo("[REDACTED]");
        assertThat(guardian.get("identityNumber")).isEqualTo("[REDACTED]");
    }
}
