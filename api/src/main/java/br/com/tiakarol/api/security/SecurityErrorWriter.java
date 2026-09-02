package br.com.tiakarol.api.security;

import br.com.tiakarol.api.configuration.ApiErrorResponse;
import br.com.tiakarol.api.configuration.RequestCorrelationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, 401, "UNAUTHORIZED", "Autenticação necessária ou token inválido.");
    }

    void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, 403, "FORBIDDEN", "Seu perfil não permite esta operação.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status,
                       String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(status, code, message,
                request.getRequestURI(), RequestCorrelationFilter.requestId(request)));
    }
}
