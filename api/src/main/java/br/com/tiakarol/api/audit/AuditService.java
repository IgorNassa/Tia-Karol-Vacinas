package br.com.tiakarol.api.audit;

import br.com.tiakarol.api.security.CurrentUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final TypeReference<Map<String, Object>> AUDIT_DATA_TYPE = new TypeReference<>() { };
    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "accesstoken", "refreshtoken", "identitynumber", "fullname", "email", "phone",
            "birthdate", "allergies", "reactions", "notes", "postalcode", "street", "number", "complement",
            "district", "city", "ibgecode");

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    AuditService(AuditLogRepository repository, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    public void log(String entityType, UUID entityId, String action, Object beforeData, Object afterData, String reason) {
        logAs(currentUser.id(), entityType, entityId, action, beforeData, afterData, reason);
    }

    public void logAs(UUID actorId, String entityType, UUID entityId, String action, Object beforeData,
                      Object afterData, String reason) {
        repository.save(new AuditLog(actorId, entityType, entityId, action, convert(beforeData),
                convert(afterData), reason));
    }

    Map<String, Object> convert(Object source) {
        if (source == null) return null;
        return sanitizeMap(objectMapper.convertValue(source, AUDIT_DATA_TYPE));
    }

    private Map<String, Object> sanitizeMap(Map<String, Object> source) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> sanitized.put(key,
                SENSITIVE_FIELDS.contains(normalize(key)) && value != null ? REDACTED : sanitizeValue(value)));
        return sanitized;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> converted.put(String.valueOf(key), nestedValue));
            return sanitizeMap(converted);
        }
        if (value instanceof List<?> list) return list.stream().map(this::sanitizeValue).toList();
        return value;
    }

    private String normalize(String field) {
        return field.replaceAll("[^A-Za-z0-9]", "").toLowerCase(java.util.Locale.ROOT);
    }
}
