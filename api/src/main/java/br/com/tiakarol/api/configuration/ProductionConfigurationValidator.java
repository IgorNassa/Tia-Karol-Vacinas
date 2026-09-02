package br.com.tiakarol.api.configuration;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
class ProductionConfigurationValidator implements SmartInitializingSingleton {
    private final String databaseUrl;
    private final String databasePassword;
    private final CorsSettings corsSettings;
    private final int authRateLimit;

    ProductionConfigurationValidator(@Value("${spring.datasource.url}") String databaseUrl,
                                     @Value("${spring.datasource.password}") String databasePassword,
                                     CorsSettings corsSettings,
                                     @Value("${app.rate-limit.auth-requests-per-minute}") int authRateLimit) {
        this.databaseUrl = databaseUrl;
        this.databasePassword = databasePassword;
        this.corsSettings = corsSettings;
        this.authRateLimit = authRateLimit;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Produção exige uma URL JDBC PostgreSQL.");
        }
        String normalizedUrl = databaseUrl.toLowerCase(java.util.Locale.ROOT);
        if (!(normalizedUrl.contains("sslmode=require") || normalizedUrl.contains("sslmode=verify-ca")
                || normalizedUrl.contains("sslmode=verify-full"))) {
            throw new IllegalStateException("Produção exige SSL explícito na conexão PostgreSQL.");
        }
        if (databasePassword.isBlank() || databasePassword.startsWith("SUBSTITUA_")) {
            throw new IllegalStateException("Senha real do banco não foi configurada.");
        }
        if (corsSettings.allowedOrigins().stream().anyMatch(origin -> origin.contains("localhost"))) {
            throw new IllegalStateException("Produção não permite origem CORS localhost.");
        }
        if (corsSettings.allowedOrigins().stream().anyMatch(origin -> !origin.startsWith("https://"))) {
            throw new IllegalStateException("Produção exige HTTPS em todas as origens CORS.");
        }
        if (authRateLimit < 5 || authRateLimit > 120) {
            throw new IllegalStateException("Limite de autenticação em produção deve estar entre 5 e 120 por minuto.");
        }
    }
}
