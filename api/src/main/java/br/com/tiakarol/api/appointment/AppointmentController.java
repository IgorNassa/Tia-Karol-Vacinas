package br.com.tiakarol.api.appointment;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
class AppointmentController {
    private final AppointmentService service;

    AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/appointments/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    AppointmentResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping
    Page<AppointmentResponse> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/pending")
    Page<AppointmentResponse> pending(Pageable pageable) { return service.pending(pageable); }

    @PatchMapping("/{id}/confirmation")
    AppointmentResponse confirm(@PathVariable UUID id) { return service.confirm(id); }

    @PatchMapping("/{id}/application")
    AppointmentResponse apply(@PathVariable UUID id, @Valid @RequestBody ApplyAppointmentRequest request) {
        return service.apply(id, request);
    }

    @PatchMapping("/{id}/cancellation")
    AppointmentResponse cancel(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return service.cancel(id, request.reason());
    }

    @PatchMapping("/{id}/no-show")
    AppointmentResponse markNoShow(@PathVariable UUID id) { return service.markNoShow(id); }

    @PatchMapping("/{id}/no-show-resolution")
    AppointmentResponse resolveNoShow(@PathVariable UUID id,
                                      @Valid @RequestBody ResolveNoShowRequest request) {
        return service.resolveNoShow(id, request);
    }
}
