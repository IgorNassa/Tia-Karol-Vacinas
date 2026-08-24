package br.com.tiakarol.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, BearerTokenAuthenticationFilter bearerFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), "Acesso negado.")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/patients/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/payments/void").hasRole("ADMIN")
                        .requestMatchers("/api/v1/appointments/*/payments/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/*/no-show-resolution").hasRole("ADMIN")
                        .requestMatchers("/api/v1/appointments/**").hasAnyRole("ADMIN", "ATTENDANT", "APPLICATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vaccine-lots/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers("/api/v1/vaccine-lots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vaccines/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers("/api/v1/vaccines/**").hasRole("ADMIN")
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
