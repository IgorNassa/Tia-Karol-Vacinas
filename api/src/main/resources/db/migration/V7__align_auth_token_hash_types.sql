ALTER TABLE auth_sessions
    ALTER COLUMN access_token_hash TYPE VARCHAR(64) USING TRIM(access_token_hash),
    ALTER COLUMN refresh_token_hash TYPE VARCHAR(64) USING TRIM(refresh_token_hash);
