package br.com.tiakarol.api.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "access_token_hash", nullable = false, unique = true, length = 64)
    private String accessTokenHash;
    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;
    @Column(name = "access_expires_at", nullable = false)
    private OffsetDateTime accessExpiresAt;
    @Column(name = "refresh_expires_at", nullable = false)
    private OffsetDateTime refreshExpiresAt;
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    protected AuthSession() { }

    AuthSession(UUID userId, String accessTokenHash, String refreshTokenHash,
                OffsetDateTime accessExpiresAt, OffsetDateTime refreshExpiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.createdAt = OffsetDateTime.now();
        this.lastUsedAt = this.createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    OffsetDateTime getAccessExpiresAt() { return accessExpiresAt; }
    OffsetDateTime getRefreshExpiresAt() { return refreshExpiresAt; }

    void markUsed() {
        lastUsedAt = OffsetDateTime.now();
    }

    void revoke() {
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }
}
