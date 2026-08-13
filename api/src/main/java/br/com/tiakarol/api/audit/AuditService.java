package br.com.tiakarol.api.audit;

import br.com.tiakarol.api.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    AuditService(AuditLogRepository repository, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    public void log(String entityType, UUID entityId, String action, Object beforeData, Object afterData, String reason) {
        repository.save(new AuditLog(currentUser.id(), entityType, entityId, action, convert(beforeData),
                convert(afterData), reason));
    }

    private Map<String, Object> convert(Object source) {
        return source == null ? null : objectMapper.convertValue(source, Map.class);
    }
}
