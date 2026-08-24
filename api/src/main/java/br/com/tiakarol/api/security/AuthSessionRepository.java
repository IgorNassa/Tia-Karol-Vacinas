package br.com.tiakarol.api.security;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByAccessTokenHashAndRevokedAtIsNullAndAccessExpiresAtAfter(
            String accessTokenHash, OffsetDateTime now);

    Optional<AuthSession> findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(
            String refreshTokenHash, OffsetDateTime now);

    @Modifying
    @Query("update AuthSession session set session.revokedAt = CURRENT_TIMESTAMP "
            + "where session.userId = :userId and session.revokedAt is null")
    int revokeAllByUserId(@Param("userId") UUID userId);
}
