package br.com.tiakarol.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {
    @Test
    void limitsOnlyAuthenticationWritesPerIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1,
                Clock.fixed(Instant.parse("2026-08-27T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper().findAndRegisterModules());

        MockHttpServletRequest first = loginRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());
        assertThat(firstResponse.getStatus()).isEqualTo(200);

        MockHttpServletRequest second = loginRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(secondResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.setAttribute(RequestCorrelationFilter.ATTRIBUTE, "request-12345678");
        return request;
    }
}
