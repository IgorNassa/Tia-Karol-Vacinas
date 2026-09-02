package br.com.tiakarol.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CorsSettingsTest {
    @Test
    void parsesDistinctExactOrigins() {
        assertThat(CorsSettings.parse("https://app.example.com, https://admin.example.com,https://app.example.com")
                .allowedOrigins()).containsExactly("https://app.example.com", "https://admin.example.com");
    }

    @Test
    void rejectsWildcardAndPath() {
        assertThatThrownBy(() -> CorsSettings.parse("*")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CorsSettings.parse("https://example.com/path"))
                .isInstanceOf(IllegalStateException.class);
    }
}
