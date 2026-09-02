package br.com.tiakarol.api.configuration;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(OffsetDateTime timestamp, int status, String code, String message,
                               String path, String requestId, Map<String, String> details) {
    public ApiErrorResponse {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static ApiErrorResponse of(int status, String code, String message, String path, String requestId) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, code, message, path, requestId, Map.of());
    }
}
