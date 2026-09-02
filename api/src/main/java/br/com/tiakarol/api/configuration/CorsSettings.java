package br.com.tiakarol.api.configuration;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

record CorsSettings(List<String> allowedOrigins) {
    static CorsSettings parse(String rawOrigins) {
        List<String> origins = Arrays.stream(rawOrigins.split(","))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (origins.isEmpty()) throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve informar ao menos uma origem.");
        for (String origin : origins) validate(origin);
        return new CorsSettings(origins);
    }

    private static void validate(String origin) {
        if (origin.contains("*")) throw new IllegalStateException("CORS não aceita origem curinga com credenciais.");
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Origem CORS inválida: " + origin, exception);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new IllegalStateException("Origem CORS deve conter apenas esquema e host: " + origin);
        }
    }
}
