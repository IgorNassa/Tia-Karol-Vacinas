package br.com.tiakarol.api.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_users")
class ApplicationUser {
    @Id
    private UUID id;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ApplicationUser() { }

    ApplicationUser(String fullName, String email, String passwordHash, UserRole role) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    UUID getId() { return id; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    UserRole getRole() { return role; }
    boolean isActive() { return active; }
}
