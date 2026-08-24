package br.com.tiakarol.api.security;

import java.time.OffsetDateTime;

public record TokenResponse(String tokenType, String accessToken, OffsetDateTime accessExpiresAt,
                            String refreshToken, OffsetDateTime refreshExpiresAt) { }
