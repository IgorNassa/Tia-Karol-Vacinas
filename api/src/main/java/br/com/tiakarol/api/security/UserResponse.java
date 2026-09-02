package br.com.tiakarol.api.security;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, UserRole role, boolean active,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
