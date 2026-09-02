package br.com.tiakarol.api.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionConfigurationValidatorTest {
    @Test
    void acceptsSecureConfiguration() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                "jdbc:postgresql://db.example.com/postgres?sslmode=require", "strong-secret",
                CorsSettings.parse("https://app.example.com"), 20);
        assertThatCode(validator::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void rejectsInsecureDatabaseAndCors() {
        assertThatThrownBy(() -> new ProductionConfigurationValidator(
                "jdbc:postgresql://db.example.com/postgres", "strong-secret",
                CorsSettings.parse("https://app.example.com"), 20).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SSL");
        assertThatThrownBy(() -> new ProductionConfigurationValidator(
                "jdbc:postgresql://db.example.com/postgres?sslmode=require", "strong-secret",
                CorsSettings.parse("http://localhost:3000"), 20).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("localhost");
        assertThatThrownBy(() -> new ProductionConfigurationValidator(
                "jdbc:postgresql://db.example.com/postgres?sslmode=require", "strong-secret",
                CorsSettings.parse("http://app.example.com"), 20).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTPS");
    }
}
