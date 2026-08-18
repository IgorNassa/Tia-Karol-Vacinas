package br.com.tiakarol.api.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
class AuditLog {
    @Id
    private UUID id;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(nullable = false)
    private String action;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private Map<String, Object> beforeData;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private Map<String, Object> afterData;
    private String reason;
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected AuditLog() { }

    AuditLog(UUID actorId, String entityType, UUID entityId, String action, Map<String, Object> beforeData,
             Map<String, Object> afterData, String reason) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.reason = reason;
        this.occurredAt = OffsetDateTime.now();
    }
}
