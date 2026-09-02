package br.com.tiakarol.api.integration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeIT {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exposesHealthAndVersionedOpenApi() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", not(blankOrNullString())));
        String generatedContract = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/v1/patients']").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String frozenContract = Files.readString(Path.of("openapi/tia-karol-api-v1.json"));
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(generatedContract))
                .as("O contrato OpenAPI mudou; revise e versione a alteração conscientemente.")
                .isEqualTo(objectMapper.readTree(frozenContract));
    }

    @Test
    void returnsSafeUniformAuthenticationErrors() throws Exception {
        mvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId", not(blankOrNullString())))
                .andExpect(jsonPath("$.path").value("/api/v1/patients"));
    }

    @Test
    void enforcesApplicatorBoundaryInRealFilterChain() throws Exception {
        mvc.perform(patch("/api/v1/appointments/00000000-0000-0000-0000-000000000001/cancellation")
                        .with(user("applicator").roles("APPLICATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"teste\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mvc.perform(get("/api/v1/appointments")
                        .with(user("applicator").roles("APPLICATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void keepsFinancialDataInvisibleToAttendant() throws Exception {
        mvc.perform(get("/api/v1/financial/entries")
                        .with(user("attendant").roles("ATTENDANT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reports/financial/summary")
                        .with(user("attendant").roles("ATTENDANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void appliesGlobalErrorContract() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isMap());
        mvc.perform(get("/api/v1/not-found").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void allowsOnlyConfiguredCorsOrigin() throws Exception {
        mvc.perform(options("/api/v1/patients")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/v1/patients")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
