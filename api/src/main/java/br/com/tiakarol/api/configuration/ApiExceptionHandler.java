package br.com.tiakarol.api.configuration;

import br.com.tiakarol.api.patient.PatientDomainException;
import br.com.tiakarol.api.stock.StockDomainException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(PatientDomainException.class)
    ResponseEntity<Map<String, Object>> patientDomain(PatientDomainException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(StockDomainException.class)
    ResponseEntity<Map<String, Object>> stockDomain(StockDomainException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        Map<String, String> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "inválido" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        return ResponseEntity.badRequest().body(Map.of("timestamp", OffsetDateTime.now(), "status", 400,
                "message", "Erro de validação.", "details", details));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", OffsetDateTime.now(), "status", status.value(),
                "message", message));
    }
}
