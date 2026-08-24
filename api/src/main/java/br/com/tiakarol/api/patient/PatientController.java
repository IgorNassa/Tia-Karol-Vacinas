package br.com.tiakarol.api.patient;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
class PatientController {
    private final PatientService service;

    PatientController(PatientService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        PatientResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/patients/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    PatientResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    Page<PatientResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return service.list(search, pageable);
    }

    @PutMapping("/{id}")
    PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/inactivation")
    PatientResponse inactivate(@PathVariable UUID id,
                               @RequestBody PatientInactivationRequest request) {
        return service.inactivate(id, request);
    }

    @GetMapping("/{id}/inactivation-preview")
    PatientInactivationPreview previewInactivation(@PathVariable UUID id) {
        return service.previewInactivation(id);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
