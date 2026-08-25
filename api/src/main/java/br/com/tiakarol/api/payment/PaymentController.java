package br.com.tiakarol.api.payment;

import br.com.tiakarol.api.appointment.ReasonRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/payments")
class PaymentController {
    private final PaymentService service;

    PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    PaymentCartResponse get(@PathVariable UUID appointmentId) {
        return service.get(appointmentId);
    }

    @GetMapping("/history")
    PaymentHistoryResponse history(@PathVariable UUID appointmentId) {
        return service.history(appointmentId);
    }

    @PutMapping
    PaymentCartResponse replace(@PathVariable UUID appointmentId,
                                @Valid @RequestBody PaymentCartRequest request) {
        return service.replace(appointmentId, request);
    }

    @PostMapping("/void")
    @ResponseStatus(HttpStatus.OK)
    PaymentCartResponse voidCart(@PathVariable UUID appointmentId,
                                 @Valid @RequestBody ReasonRequest request) {
        return service.voidCart(appointmentId, request.reason());
    }
}
