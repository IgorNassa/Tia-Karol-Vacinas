package br.com.tiakarol.api.configuration;

import br.com.tiakarol.api.appointment.AppointmentDomainException;
import br.com.tiakarol.api.finance.FinancialDomainException;
import br.com.tiakarol.api.patient.PatientDomainException;
import br.com.tiakarol.api.payment.PaymentDomainException;
import br.com.tiakarol.api.security.AuthenticationDomainException;
import br.com.tiakarol.api.security.SecurityDomainException;
import br.com.tiakarol.api.stock.StockDomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({PatientDomainException.class, StockDomainException.class,
            AppointmentDomainException.class, PaymentDomainException.class, FinancialDomainException.class,
            SecurityDomainException.class})
    ResponseEntity<ApiErrorResponse> domain(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationDomainException.class)
    ResponseEntity<ApiErrorResponse> authentication(AuthenticationDomainException exception,
                                                     HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception,
                                                HttpServletRequest request) {
        Map<String, String> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "inválido" : fieldError.getDefaultMessage(),
                        (first, ignored) -> first));
        ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), 400, "VALIDATION_ERROR",
                "Erro de validação.", request.getRequestURI(), RequestCorrelationFilter.requestId(request), details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> malformedRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "A requisição contém campos ausentes ou inválidos.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception,
                                                  HttpServletRequest request) {
        LOGGER.warn("Conflito de integridade em requestId={}", RequestCorrelationFilter.requestId(request));
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "A operação conflita com dados já cadastrados.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(NoResourceFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                      HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Método HTTP não permitido para este recurso.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        String requestId = RequestCorrelationFilter.requestId(request);
        LOGGER.error("Erro não tratado em requestId={}", requestId, exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro interno. Informe o identificador da requisição ao suporte.", request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), code, message,
                request.getRequestURI(), RequestCorrelationFilter.requestId(request)));
    }
}
