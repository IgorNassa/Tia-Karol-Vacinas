package br.com.tiakarol.api.configuration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
class WebSecuritySupportConfiguration {
    @Bean
    CorsSettings corsSettings(@Value("${app.cors.allowed-origins:http://localhost:3000}") String origins) {
        return CorsSettings.parse(origins);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsSettings settings) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(settings.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", RequestCorrelationFilter.HEADER));
        configuration.setExposedHeaders(List.of(RequestCorrelationFilter.HEADER));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/health/**", configuration);
        return source;
    }
}
