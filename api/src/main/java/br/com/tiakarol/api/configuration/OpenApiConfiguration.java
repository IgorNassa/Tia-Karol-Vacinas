package br.com.tiakarol.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI tiaKarolOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Tia Karol Vacinas API")
                .version("v1")
                .description("API para pacientes, lotes, estoque, aplicações e financeiro."));
    }
}
