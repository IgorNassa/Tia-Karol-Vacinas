package br.com.tiakarol.api.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class AuthRateLimitFilter extends OncePerRequestFilter {
    private final int requestsPerMinute;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    AuthRateLimitFilter(@Value("${app.rate-limit.auth-requests-per-minute:20}") int requestsPerMinute,
                        ObjectMapper objectMapper) {
        this(requestsPerMinute, Clock.systemUTC(), objectMapper);
    }

    AuthRateLimitFilter(int requestsPerMinute, Clock clock, ObjectMapper objectMapper) {
        if (requestsPerMinute < 1) throw new IllegalArgumentException("Limite de autenticação deve ser positivo.");
        this.requestsPerMinute = requestsPerMinute;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !("POST".equals(request.getMethod())
                && ("/api/v1/auth/login".equals(path) || "/api/v1/auth/refresh".equals(path)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long minute = clock.millis() / 60_000L;
        String key = request.getRemoteAddr() + '|' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) -> current == null || current.minute() != minute
                ? new Window(minute, 1) : new Window(minute, current.count() + 1));
        if (minute % 10 == 0) windows.entrySet().removeIf(entry -> entry.getValue().minute() < minute - 1);
        if (window.count() <= requestsPerMinute) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(429, "RATE_LIMIT_EXCEEDED",
                "Muitas tentativas de autenticação. Tente novamente em instantes.", request.getRequestURI(),
                RequestCorrelationFilter.requestId(request)));
    }

    private record Window(long minute, int count) {
    }
}
